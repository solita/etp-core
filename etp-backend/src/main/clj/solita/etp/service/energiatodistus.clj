(ns solita.etp.service.energiatodistus
  (:require [solita.etp.db :as db]
            [solita.etp.exception :as exception]
            [solita.etp.schema.energiatodistus :as energiatodistus-schema]
            [solita.etp.schema.geo :as geo-schema]
            [solita.etp.service.json :as json]
            [solita.etp.service.energiatodistus-validation :as validation]
            [solita.etp.service.kayttotarkoitus :as kayttotarkoitus-service]
            [solita.etp.service.rooli :as rooli-service]
            [solita.postgresql.composite :as pg-composite]
            [solita.common.schema :as xschema]
            [schema.coerce :as coerce]
            [clojure.java.jdbc :as jdbc]
            [clojure.set :as set]
            [flathead.flatten :as flat]
            [clojure.string :as str]
            [clojure.core.match :as match]
            [schema.core :as schema]
            [solita.common.map :as map]
            [solita.common.logic :as logic]
            [schema-tools.coerce :as stc]))

; *** Require sql functions ***
(db/require-queries 'energiatodistus)

; *** Conversions from database data types ***
(def coerce-energiatodistus (coerce/coercer! energiatodistus-schema/Energiatodistus
                                            (stc/or-matcher
                                              stc/map-filter-matcher
                                              (assoc json/json-coercions
                                                geo-schema/PostinumeroFI (logic/unless* nil? #(format "%05d" %))
                                                schema/Num xschema/parse-big-decimal))))

(def ^:private tilat [:draft :in-signing :signed :discarded :replaced :deleted])

(defn tila-key [tila-id] (nth tilat tila-id))

(def db-abbreviations
  {:perustiedot :pt
   :lahtotiedot :lt
   :tulokset :t
   :toteutunut-ostoenergiankulutus :to
   :huomiot                        :h})

(def db-toimenpide-type
  [:nimi-fi
   :nimi-sv
   :lampo
   :sahko
   :jaahdytys
   :eluvun-muutos])

(def db-userdefined_energiamuoto-type
  [:nimi
   :muotokerroin
   :ostoenergia])

(def db-userdefined_energia-type
  [:nimi-fi
   :nimi-sv
   :vuosikulutus])

(def db-kuukausierittely-type
  [:tuotto$aurinkosahko
   :tuotto$tuulisahko
   :tuotto$aurinkolampo
   :tuotto$muulampo
   :tuotto$muusahko
   :tuotto$lampopumppu
   :kulutus$sahko
   :kulutus$lampo])

(def db-ostettu-polttoaine-type
  [:nimi
   :yksikko
   :muunnoskerroin
   :maara-vuodessa])

(def db-composite-types
  {:t$kaytettavat-energiamuodot$muu    db-userdefined_energiamuoto-type
   :t$uusiutuvat-omavaraisenergiat$muu db-userdefined_energia-type
   :t$kuukausierittely                 db-kuukausierittely-type
   :to$ostettu-energia$muu             db-userdefined_energia-type
   :to$ostetut-polttoaineet$muu        db-ostettu-polttoaine-type
   :h$iv-ilmastointi$toimenpide        db-toimenpide-type
   :h$valaistus-muut$toimenpide        db-toimenpide-type
   :h$lammitys$toimenpide              db-toimenpide-type
   :h$ymparys$toimenpide               db-toimenpide-type
   :h$alapohja-ylapohja$toimenpide     db-toimenpide-type})

(defn convert-db-case [name]
  (-> name
      (str/replace #"\$u$" "\\$U")
      (str/replace #"\-ua$" "-UA")))

(defn convert-db-key-case [key]
  (-> key
      name
      convert-db-case
      keyword))

(defn- find-numeric-column-validations [db versio]
  (->>
    (energiatodistus-db/select-numeric-validations db {:versio versio})
    (map db/kebab-case-keys)
    (map #(flat/flat->tree #"\$" %))))

(defn replace-abbreviation->fullname [path]
  (reduce (fn [result [fullname abbreviation]]
            (if (str/starts-with? result (name abbreviation))
              (reduced (str/replace-first
                         result (name abbreviation) (name fullname)))
              result))
          path db-abbreviations))

(defn to-property-name [column-name]
  (-> column-name
      db/kebab-case
      convert-db-case
      replace-abbreviation->fullname
      (str/replace "$" ".")))

(defn find-numeric-validations [db versio]
  (map (comp
         #(set/rename-keys % {:column-name :property})
         #(update % :column-name to-property-name))
       (find-numeric-column-validations db versio)))

(defn validate-db-row! [energiatodistus db versio]
  (doseq [{{:keys [min max]} :error :keys [column-name]}
          (find-numeric-column-validations db versio)]
    (if-let [value ((keyword column-name) energiatodistus)]
      (when (or (< value min) (> value max))
        (exception/throw-ex-info!
          :invalid-value
          (str "Property: " (to-property-name column-name)
               " has an invalid value: " value)))))
  energiatodistus)

(defn flat->tree [energiatodistus]
  (->> energiatodistus
    (map/map-values (logic/when* vector? (partial mapv flat->tree)))
    (flat/flat->tree #"\$")))

(defn find-sisaiset-kuormat [db versio]
  (map (comp flat->tree db/kebab-case-keys)
       (energiatodistus-db/select-sisaiset-kuormat db {:versio versio})))

(def db-row->energiatodistus
  (comp coerce-energiatodistus
        (logic/when*
          #(= (:versio %) 2013)
          #(update-in % [:tulokset :uusiutuvat-omavaraisenergiat] :muu))
        #(set/rename-keys % (set/map-invert db-abbreviations))
        flat->tree
        (partial pg-composite/parse-composite-type-literals db-composite-types)
        (partial map/map-keys convert-db-key-case)
        db/kebab-case-keys))

(defn tree->flat [energiatodistus]
  (->> energiatodistus
       (flat/tree->flat "$")
       (map/map-values (logic/when* vector? (partial mapv tree->flat)))))

(defn- parseInt [str] (Integer/parseInt str))

(def energiatodistus->db-row
  (comp
    (partial pg-composite/write-composite-type-literals db-composite-types)
    tree->flat
    #(set/rename-keys % db-abbreviations)
    #(update-in % [:perustiedot :postinumero] (logic/unless* nil? parseInt))
    (logic/when*
      #(= (:versio %) 2013)
      #(update-in % [:tulokset :uusiutuvat-omavaraisenergiat] (partial assoc {} :muu)))))

;; TODO this could be later modified to handle deleted energiatodistus
;; by returning nil (turned 404 in api) and throwing forbidden
;; if role is paakayttaja and energiatodistus is draft
(defn- select-energiatodistus-for-find
  [{:keys [id tila-id] :as energiatodistus} rooli]
  (match/match
   [(tila-key tila-id) rooli]
   [_ :laatija] (dissoc energiatodistus :kommentti)
   [_ :paakayttaja] energiatodistus))

(defn find-energiatodistus
  ([db id]
   (first (map db-row->energiatodistus
               (energiatodistus-db/select-energiatodistus db {:id id}))))
  ([db whoami id]
   (if-let [energiatodistus (find-energiatodistus db id)]
     (if (or (rooli-service/paakayttaja? whoami)
             (and (rooli-service/laatija? whoami)
                  (= (:laatija-id energiatodistus) (:id whoami))))
       (select-energiatodistus-for-find
        energiatodistus
        (-> whoami :rooli rooli-service/rooli-key))
       (exception/throw-forbidden!)))))

(defn find-replaceable-energiatodistukset-like-id [db id]
  (map :id (energiatodistus-db/select-replaceable-energiatodistukset-like-id db {:id id})))

(defn assert-korvaavuus! [db energiatodistus]
  (when-let [korvattu-energiatodistus-id (:korvattu-energiatodistus-id energiatodistus)]
    (if-let [korvattava-energiatodistus (find-energiatodistus db korvattu-energiatodistus-id)]
      (cond
        (and (:korvaava-energiatodistus-id korvattava-energiatodistus)
             (not= (:korvaava-energiatodistus-id korvattava-energiatodistus) (:id energiatodistus)))
        (exception/throw-ex-info! :invalid-replace "Replaceable energiatodistus is already replaced")
        (not (contains? #{:signed :discarded} (-> korvattava-energiatodistus :tila-id tila-key)))
        (exception/throw-ex-info! :invalid-replace "Replaceable energiatodistus is not in signed or discarded state"))
      (exception/throw-ex-info! :invalid-replace "Replaceable energiatodistus does not exists"))))

(defn validate-sisainen-kuorma!
  [db versio energiatodistus]
  (validation/validate-sisainen-kuorma!
    (find-sisaiset-kuormat db versio)
    (kayttotarkoitus-service/find-alakayttotarkoitukset db versio)
    energiatodistus))

(defn add-energiatodistus! [db whoami versio energiatodistus]
  (assert-korvaavuus! db energiatodistus)
  (validate-sisainen-kuorma! db versio energiatodistus)
  (-> (db/with-db-exception-translation
        jdbc/insert! db
        :energiatodistus
        (-> energiatodistus
            (assoc :versio versio
                   :laatija-id (:id whoami))
            (dissoc :kommentti)
            energiatodistus->db-row
            (validate-db-row! db versio))
        db/default-opts)
      first
      :id))

(defn assert-laatija! [whoami energiatodistus]
  (when (and (not= (:laatija-id energiatodistus) (:id whoami))
             (rooli-service/laatija? whoami))
    (exception/throw-forbidden!
      (str "User " (:id whoami)
           " is not the laatija of energiatodistus: "
           (:id energiatodistus)))))

(defn- select-energiatodistus-for-update
  [energiatodistus-update
   id tila-id rooli laskutettu?]

  (match/match
    [(tila-key tila-id) rooli laskutettu?]
    [:draft :laatija false] (dissoc energiatodistus-update :kommentti)
    [:signed :laatija false]
    (select-keys energiatodistus-update
                 [:laskutettava-yritys-id
                  :laskuriviviite
                  :pt$rakennustunnus])
    [:signed :laatija true]
    (select-keys energiatodistus-update
                 [:pt$rakennustunnus])
    [:signed :paakayttaja false]
    (select-keys energiatodistus-update
                 [:laskutettava-yritys-id
                  :laskuriviviite
                  :pt$rakennustunnus
                  :korvattu-energiatodistus-id
                  :kommentti])
    [:signed :paakayttaja true]
    (select-keys energiatodistus-update
                 [:pt$rakennustunnus
                  :korvattu-energiatodistus-id
                  :kommentti])
    :else (exception/throw-forbidden!
            (str "Role: " rooli
                 " is not allowed to update energiatodistus " id
                 " in state: " (tila-key tila-id) " laskutettu: " laskutettu?))))

(defn- db-update-energiatodistus! [db id versio energiatodistus
                                   tila-id rooli laskutettu?]
  (first (db/with-db-exception-translation jdbc/update!
           db :energiatodistus
           (-> energiatodistus
               (assoc :versio versio)
               energiatodistus->db-row
               (dissoc :versio)
               (select-energiatodistus-for-update id tila-id rooli laskutettu?)
               (validate-db-row! db versio))
           ["id = ? and tila_id = ? and (laskutusaika is not null) = ?"
            id tila-id laskutettu?]
           db/default-opts)))

(defn- assert-update! [id result]
  (if (== result 0)
    (exception/throw-ex-info!
      :update-conflict
      (str "Energiatodistus " id
           " update conflicts with other concurrent update."))
    result))

(defn find-required-properties [db versio]
  (map (comp to-property-name :column-name)
       (energiatodistus-db/select-required-columns db {:versio versio})))

(defn find-required-constraints [db energiatodistus]
  (->> energiatodistus
      :versio
      (find-required-properties db)
      validation/required-constraints))

(defn validate-required!
  ([db current update]
   (validation/validate-required!
     (find-required-constraints db current)
     current update))
  ([db energiatodistus]
   (validation/validate-required!
     (find-required-constraints db energiatodistus)
     energiatodistus)))

(defn update-energiatodistus! [db whoami id energiatodistus]
  (if-let [current-energiatodistus (find-energiatodistus db id)]
    (let [tila-id (:tila-id current-energiatodistus)
          rooli (-> whoami :rooli rooli-service/rooli-key)
          laskutettu? (-> current-energiatodistus :laskutusaika ((complement nil?)))]
      (assert-laatija! whoami current-energiatodistus)
      (assert-korvaavuus! db (assoc energiatodistus :id id))
      (when (not= (tila-key tila-id) :draft)
        (validate-required! db
          current-energiatodistus
          energiatodistus))
      (when (= (tila-key tila-id) :draft)
        (validate-sisainen-kuorma!
          db (:versio current-energiatodistus) energiatodistus))
      (assert-update! id
        (db-update-energiatodistus!
          db id (:versio current-energiatodistus)
          energiatodistus
          tila-id rooli laskutettu?))
      nil)
    (exception/throw-ex-info!
      :not-found
      (str "Energiatodistus " id " does not exists."))))

(defn delete-energiatodistus-luonnos! [db whoami id]
  (assert-laatija! whoami (find-energiatodistus db id))
  (energiatodistus-db/delete-energiatodistus-luonnos! db {:id id}))

;;
;; Signing process
;;

(defn start-energiatodistus-signing! [db whoami id]
  (jdbc/with-db-transaction [db db]
    (let [result (energiatodistus-db/update-energiatodistus-allekirjoituksessa!
                   db {:id id :laatija-id (:id whoami)})
          energiatodistus (find-energiatodistus db id)]
      (if (= result 1)
        (do
          (validate-required! db energiatodistus)
          :ok)
        (when-let [{:keys [tila-id]} energiatodistus]
          (assert-laatija! whoami energiatodistus)
          (case (tila-key tila-id)
            :in-signing :already-in-signing
            :deleted nil
            :already-signed))))))

(defn mark-energiatodistus-as-korvattu! [db whoami id]
  (let [result (energiatodistus-db/update-energiatodistus-korvattu!
                 db {:id id :laatija-id (:id whoami)})]
    (when (= result 1)
      :ok)))

(defn- failure-code [db whoami id]
  (when-let [{:keys [tila-id] :as et} (find-energiatodistus db id)]
    (assert-laatija! whoami et)
    (case (tila-key tila-id)
      :draft :not-in-signing
      :deleted nil
      :already-signed)))

(defn end-energiatodistus-signing! [db whoami id]
  (jdbc/with-db-transaction [db db]
    (let [result (energiatodistus-db/update-energiatodistus-allekirjoitettu!
                   db {:id id :laatija-id (:id whoami)})]
      (if (= result 1)
        (if-let [korvattu-energiatodistus-id (:korvattu-energiatodistus-id (find-energiatodistus db id))]
          (mark-energiatodistus-as-korvattu! db whoami korvattu-energiatodistus-id)
          :ok)
        (failure-code db whoami id)))))

(defn cancel-energiatodistus-signing! [db whoami id]
  (let [result (energiatodistus-db/update-energiatodistus-luonnos!
                 db {:id id :laatija-id (:id whoami)})]
    (if (= result 1) :ok (failure-code db whoami id))))
