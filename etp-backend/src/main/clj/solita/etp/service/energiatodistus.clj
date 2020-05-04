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

(defn find-energiatodistukset-by-laatija [db laatija-id]
  (map (comp coerce-energiatodistus json/merge-data)
       (energiatodistus-db/select-energiatodistukset-by-laatija db {:laatija-id laatija-id})))

(defn add-energiatodistus! [db whoami versio energiatodistus]
  (:id (energiatodistus-db/insert-energiatodistus<!
         db (assoc (json/data-db-row energiatodistus)
              :versio versio
              :laatija-id (:laatija whoami)))))

(defn update-energiatodistus-luonnos! [db id energiatodistus]
  (energiatodistus-db/update-energiatodistus-luonnos!
    db {:id id :data (json/write-value-as-string energiatodistus)}))

(defn delete-energiatodistus-luonnos! [db id]
  (energiatodistus-db/delete-energiatodistus-luonnos! db {:id id}))

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
