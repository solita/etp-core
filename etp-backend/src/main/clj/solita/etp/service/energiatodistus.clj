(ns solita.etp.service.energiatodistus
  (:require [solita.etp.db :as db]
            [solita.etp.exception :as exception]
            [solita.etp.schema.energiatodistus :as energiatodistus-schema]
            [solita.etp.service.json :as json]
            [solita.etp.service.rooli :as rooli-service]
            [schema.coerce :as coerce]
            [clojure.java.jdbc :as jdbc]))

; *** Require sql functions ***
(db/require-queries 'energiatodistus)

; *** Conversions from database data types ***
(def coerce-energiatodistus (coerce/coercer energiatodistus-schema/Energiatodistus json/json-coercions))

(defn find-energiatodistus
  ([db id]
   (first (map (comp coerce-energiatodistus json/merge-data)
               (energiatodistus-db/select-energiatodistus db {:id id}))))
  ([db whoami id]
   (let [energiatodistus (find-energiatodistus db id)]
     (if (or (rooli-service/paakayttaja? whoami)
             (and (rooli-service/laatija? whoami)
                  (= (:laatija-id energiatodistus) (:laatija whoami))))
       energiatodistus
       (exception/throw-forbidden!)))))

(defn find-energiatodistukset-by-laatija [db laatija-id tila-id]
  (map (comp coerce-energiatodistus json/merge-data)
       (energiatodistus-db/select-energiatodistukset-by-laatija
         db {:laatija-id laatija-id :tila-id tila-id})))

(defn add-energiatodistus! [db whoami versio energiatodistus]
  (:id (energiatodistus-db/insert-energiatodistus<!
         db (assoc (json/data-db-row energiatodistus)
              :versio versio
              :laatija-id (:laatija whoami)))))

(defn update-energiatodistus-luonnos! [db whoami id energiatodistus]
  (let [{:keys [laatija-id]} (find-energiatodistus db id)]
    (if (= laatija-id (:laatija whoami))
      (energiatodistus-db/update-energiatodistus-luonnos!
       db
       {:id id :data (json/write-value-as-string energiatodistus)})
      (exception/throw-forbidden!))))

(defn delete-energiatodistus-luonnos! [db whoami id]
  (let [{:keys [laatija-id]} (find-energiatodistus db id)]
    (if (= laatija-id (:laatija whoami))
      (energiatodistus-db/delete-energiatodistus-luonnos! db {:id id})
      (exception/throw-forbidden!))))

(defn start-energiatodistus-signing! [db id]
  (when-let [{:keys [allekirjoituksessaaika allekirjoitusaika]}
             (find-energiatodistus db id)]
    (cond
      (-> allekirjoitusaika nil? not)
      :already-signed

      (-> allekirjoituksessaaika nil? not)
      :already-in-signing

      :else
      (do
        (energiatodistus-db/update-energiatodistus-allekirjoituksessaaika! db {:id id})
        :ok))))

(defn stop-energiatodistus-signing! [db id]
  (when-let [{:keys [allekirjoituksessaaika allekirjoitusaika]} (find-energiatodistus db id)]
    (cond
      (-> allekirjoitusaika nil? not)
      :already-signed

      (-> allekirjoituksessaaika nil?)
      :not-in-signing

      :else
      (do
        (energiatodistus-db/update-energiatodistus-allekirjoitusaika! db {:id id})
        :ok))))

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

;;
;; Energiatodistuksen käyttötarkoitusluokittelu
;;

(defn find-kayttotarkoitukset [db versio]
  (energiatodistus-db/select-kayttotarkoitusluokat-by-versio db {:versio versio}))

(defn find-alakayttotarkoitukset [db versio]
  (energiatodistus-db/select-alakayttotarkoitusluokat-by-versio db {:versio versio}))

;;
;; Energiatodistuksen "denormalisointi" ja laskennalliset kentät
;;

(defn *-ceil [& args]
  (->> args (apply *) Math/ceil))

(defn combine-keys [m f cursor-new cursor-1 cursor-2]
  (assoc-in m cursor-new (* (get-in m cursor-1) (get-in m cursor-2))))

(defn assoc-div-nettoala [energiatodistus cursor]
  (let [new-k (-> cursor last name (str "-nettoala") keyword)
        new-cursor (-> cursor pop (conj new-k))]
    (combine-keys energiatodistus
                  /
                  new-cursor cursor
                  [:lahtotiedot :lammitetty-nettoala])))

(defn find-complete-energiatodistus* [energiatodistus alakayttotarkoitukset]
  (let [kayttotarkoitus-id (get-in energiatodistus [:perustiedot :kayttotarkoitus])
         alakayttotarkoitus (->> alakayttotarkoitukset
                                 (filter #(= (:id %) kayttotarkoitus-id))
                                 first)]
     (-> energiatodistus
         (assoc-in [:perustiedot :alakayttotarkoitus-fi] (:label-fi alakayttotarkoitus))
         (assoc-in [:perustiedot :alakayttotarkoitus-sv] (:label-sv alakayttotarkoitus))
         (assoc-in [:tulokset :kaytettavat-energiamuodot :kaukolampo-kerroin] 0.5)
         (assoc-in [:tulokset :kaytettavat-energiamuodot :sahko-kerroin] 1.2)
         (assoc-in [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-kerroin] 0.5)
         (assoc-in [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-kerroin] 1)
         (assoc-in [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-kerroin] 0.28)
         (assoc-div-nettoala [:tulokset :kaytettavat-energiamuodot :kaukolampo])
         (assoc-div-nettoala [:tulokset :kaytettavat-energiamuodot :sahko])
         (assoc-div-nettoala [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine])
         (assoc-div-nettoala [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine])
         (assoc-div-nettoala [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys])
         (combine-keys *-ceil
                       [:tulokset :kaytettavat-energiamuodot :kaukolampo-nettoala-kerroin]
                       [:tulokset :kaytettavat-energiamuodot :kaukolampo-nettoala]
                       [:tulokset :kaytettavat-energiamuodot :kaukolampo-kerroin])
         (combine-keys *-ceil
                       [:tulokset :kaytettavat-energiamuodot :sahko-nettoala-kerroin]
                       [:tulokset :kaytettavat-energiamuodot :sahko-nettoala]
                       [:tulokset :kaytettavat-energiamuodot :sahko-kerroin])
         (combine-keys *-ceil
                       [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-nettoala-kerroin]
                       [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-nettoala]
                       [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-kerroin])
         (combine-keys *-ceil
                       [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-nettoala-kerroin]
                       [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-nettoala]
                       [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-kerroin])
         (combine-keys *-ceil
                       [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-nettoala-kerroin]
                       [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-nettoala]
                       [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-kerroin]))))

(defn find-complete-energiatodistus
  ([db id]
   (find-complete-energiatodistus* (find-energiatodistus db id)
                                   (find-alakayttotarkoitukset db 2018)))
  ([db whoami id]
   (find-complete-energiatodistus* (find-energiatodistus db whoami id)
                                   (find-alakayttotarkoitukset db 2018))))
