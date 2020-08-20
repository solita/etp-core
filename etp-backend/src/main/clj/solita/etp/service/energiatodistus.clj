(ns solita.etp.service.energiatodistus
  (:require [solita.etp.db :as db]
            [solita.etp.exception :as exception]
            [solita.etp.schema.energiatodistus :as energiatodistus-schema]
            [solita.etp.service.json :as json]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.service.kayttotarkoitus :as kayttotarkoitus-service]
            [solita.postgresql.composite :as pg-composite]
            [solita.common.schema :as xschema]
            [schema.coerce :as coerce]
            [clojure.java.jdbc :as jdbc]
            [clojure.set :as set]
            [flathead.flatten :as flat]
            [clojure.string :as str]
            [schema.core :as schema]
            [solita.common.map :as map]
            [solita.common.logic :as logic]
            [schema-tools.coerce :as stc]))

; *** Require sql functions ***
(db/require-queries 'energiatodistus)

; *** Conversions from database data types ***
(def coerce-energiatodistus (coerce/coercer energiatodistus-schema/Energiatodistus
                                            (stc/or-matcher
                                              stc/map-filter-matcher
                                              (assoc json/json-coercions
                                                schema/Num xschema/parse-big-decimal))))

(def tilat [:draft :in-signing :signed :discarded :replaced :deleted])

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
  {:tulokset
    {:kaytettavat-energiamuodot {:muu db-userdefined_energiamuoto-type}
     :uusiutuvat-omavaraisenergiat {:muu db-userdefined_energia-type}
     :kuukausierittely db-kuukausierittely-type}
   :toteutunut-ostoenergiankulutus
    {:ostettu-energia {:muu db-userdefined_energia-type}
     :ostetut-polttoaineet {:muu db-ostettu-polttoaine-type}}
   :huomiot
    {:iv-ilmastointi { :toimenpide db-toimenpide-type }
     :valaistus-muut { :toimenpide db-toimenpide-type }
     :lammitys { :toimenpide db-toimenpide-type }
     :ymparys { :toimenpide db-toimenpide-type }
     :alapohja-ylapohja { :toimenpide db-toimenpide-type }}})

(defn convert-db-key-case [key]
  (-> key
      name
      (str/replace #"\$u$" "\\$U")
      (str/replace #"\-ua$" "-UA")
      keyword))

(def db-row->energiatodistus
  (comp coerce-energiatodistus
        (logic/when*
          #(= (:versio %) 2013)
          #(update-in % [:tulokset :uusiutuvat-omavaraisenergiat] :muu))
        (fn [energiatodistus]
          (update-in energiatodistus
                     [:tulokset :kuukausierittely]
                     #(map (partial flat/flat->tree #"\$") %)))
        (partial pg-composite/parse-composite-type-literals db-composite-types)
        #(set/rename-keys % (set/map-invert db-abbreviations))
        (partial flat/flat->tree #"\$")
        (partial map/map-keys convert-db-key-case)
        db/kebab-case-keys))

(def energiatodistus->db-row
  (comp
    (partial flat/tree->flat "$")
    #(set/rename-keys % db-abbreviations)
    (partial pg-composite/write-composite-type-literals db-composite-types)
    (fn [energiatodistus]
      (update-in energiatodistus
                 [:tulokset :kuukausierittely]
                 #(map (partial flat/tree->flat "$") %)))
    (logic/when*
      #(= (:versio %) 2013)
      #(update-in % [:tulokset :uusiutuvat-omavaraisenergiat] (partial assoc {} :muu)))))

(defn find-energiatodistus
  ([db id]
   (first (map db-row->energiatodistus
               (energiatodistus-db/select-energiatodistus db {:id id}))))
  ([db whoami id]
   (let [energiatodistus (find-energiatodistus db id)]
     (if (or (rooli-service/paakayttaja? whoami)
             (and (rooli-service/laatija? whoami)
                  (= (:laatija-id energiatodistus) (:id whoami))))
       energiatodistus
       (exception/throw-forbidden!)))))

(defn find-energiatodistukset-by-laatija [db laatija-id tila-id]
  (map db-row->energiatodistus
       (energiatodistus-db/select-energiatodistukset-by-laatija
         db {:laatija-id laatija-id :tila-id tila-id})))

(defn find-replaceable-energiatodistukset-like-id [db id]
  (map :id (energiatodistus-db/select-replaceable-energiatodistukset-like-id db {:id id})))

(defn assert-korvaavuus! [db energiatodistus]
  (when-let [korvattu-energiatodistus-id (:korvattu-energiatodistus-id energiatodistus)]
    (if-let [korvattava-energiatodistus (find-energiatodistus db korvattu-energiatodistus-id)]
      (cond
        (:korvaava-energiatodistus-id korvattava-energiatodistus) (throw (IllegalArgumentException. "Replaceable energiatodistus is already replaced"))
        (not (contains? #{:signed :discarded} (-> korvattava-energiatodistus :tila-id tila-key))) (throw (IllegalArgumentException. "Replaceable energiatodistus is not in signed or discarded state")))
      (throw (IllegalArgumentException. (str "Replaceable energiatodistus is not exists"))))))

(defn add-energiatodistus! [db whoami versio energiatodistus]
  (assert-korvaavuus! db energiatodistus)
  (-> (jdbc/insert! db
                    :energiatodistus
                    (-> energiatodistus
                        (assoc :versio versio
                               :laatija-id (:id whoami))
                        energiatodistus->db-row)
                    db/default-opts)
      first
      :id))

(defn assert-laatija! [whoami energiatodistus]
  (when-not (= (:laatija-id energiatodistus) (:id whoami))
    (exception/throw-forbidden!
      (str "User " (:id whoami) " is not the laatija of et-" (:id energiatodistus)))))

(defn- update-energiatodistus-luonnos! [db id version energiatodistus]
  (first (jdbc/update! db :energiatodistus
                       (-> energiatodistus
                           (assoc :versio version)
                           energiatodistus->db-row
                           (dissoc :versio))
                       ["id = ? and tila_id = 0" id] db/default-opts)))

(defn update-energiatodistus! [db whoami id energiatodistus]
  (if-let [current-energiatodistus (find-energiatodistus db id)]
    (do
      (assert-laatija! whoami current-energiatodistus)
      (assert-korvaavuus! db energiatodistus)
      (case (-> current-energiatodistus :tila-id tila-key)
            :draft (update-energiatodistus-luonnos!
                     db id (:versio current-energiatodistus) energiatodistus)
            :signed (energiatodistus-db/update-rakennustunnus-when-energiatodistus-signed!
                      db {:id id
                          :rakennustunnus (-> energiatodistus :perustiedot :rakennustunnus)})
            0))
    0))

(defn delete-energiatodistus-luonnos! [db whoami id]
  (assert-laatija! whoami (find-energiatodistus db id))
  (energiatodistus-db/delete-energiatodistus-luonnos! db {:id id}))

;;
;; Signing process
;;

(defn start-energiatodistus-signing! [db whoami id]
  (let [result (energiatodistus-db/update-energiatodistus-allekirjoituksessa!
                 db {:id id :laatija-id (:id whoami)})]
    (if (= result 1) :ok
      (when-let [{:keys [tila-id] :as et} (find-energiatodistus db id)]
        (assert-laatija! whoami et)
        (case (tila-key tila-id)
          :in-signing :already-in-signing
          :deleted nil
          :already-signed)))))

(defn mark-energiatodistus-as-korvattu! [db whoami id]
  (let [result (energiatodistus-db/update-energiatodistus-korvattu!
                 db {:id id :laatija-id (:id whoami)})]
    (when (= result 1)
      :ok)))

(defn end-energiatodistus-signing! [db whoami id]
  (jdbc/with-db-transaction [db db]
    (let [result (energiatodistus-db/update-energiatodistus-allekirjoitettu!
                   db {:id id :laatija-id (:id whoami)})]
      (if (= result 1)
        (if-let [korvattu-energiatodistus-id (:korvattu-energiatodistus-id (find-energiatodistus db id))]
          (mark-energiatodistus-as-korvattu! db whoami korvattu-energiatodistus-id)
          :ok)
        (when-let [{:keys [tila-id] :as et} (find-energiatodistus db id)]
          (assert-laatija! whoami et)
          (case (tila-key tila-id)
            :draft :not-in-signing
            :deleted nil
            :already-signed))))))

;;
;; Energiatodistuksen kielisyys
;;

(def kielisyys [{:id 0 :label-fi "Suomi" :label-sv "Finska"}
                {:id 1 :label-fi "Ruotsi" :label-sv "Svenska"}
                {:id 2 :label-fi "Kaksikielinen" :label-sv "Tvåspråkig"}])

(defn find-kielisyys [] kielisyys)

;;
;; Energiatodistuksen laatimisvaihe
;;

(def laatimisvaiheet [{:id 0 :label-fi "Rakennuslupa" :label-sv "Bygglov"}
                      {:id 1 :label-fi "Käyttöönotto" :label-sv "Införandet"}
                      {:id 2 :label-fi "Olemassa oleva rakennus" :label-sv "Befintlig byggnad"}])

(defn find-laatimisvaiheet [] laatimisvaiheet)
