(ns solita.etp.service.laatija
  (:require [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [solita.common.map :as map]
            [solita.etp.db :as db]
            [solita.etp.schema.laatija :as laatija-schema]
            [solita.etp.service.json :as json]
            [solita.etp.service.kayttaja :as kayttaja-service]
            [schema.coerce :as coerce]))

; *** Require sql functions ***
(db/require-queries 'kayttaja)
(db/require-queries 'laatija)

; *** Conversions from database data types ***
(def coerce-laatija (coerce/coercer laatija-schema/Laatija json/json-coercions))

;; TODO doing this with a join instead of two queries would be more efficient
(defn find-kayttaja-laatija-with-henkilotunnus [db henkilotunnus]
  (jdbc/with-db-transaction
    [db db]
    (when-let [laatija (->> {:henkilotunnus henkilotunnus}
                            (laatija-db/select-laatija-with-henkilotunnus db)
                            (map coerce-laatija)
                            first)]
      {:kayttaja (kayttaja-service/find-kayttaja db (:kayttaja laatija))
       :laatija laatija})))

(defn- upsert-kayttaja-laatija! [db {:keys [kayttaja laatija]}]
  "Upserts käyttäjä and laatija WITHOUT transaction."
  (if-let [existing-kayttaja-laatija (find-kayttaja-laatija-with-henkilotunnus
                                      db
                                      (:henkilotunnus laatija))]
    (let [existing-kayttaja (:kayttaja existing-kayttaja-laatija)
          existing-laatija (:laatija existing-kayttaja-laatija)]
      (laatija-db/update-laatija! db  (-> existing-laatija (merge laatija)))
      {:kayttaja (:kayttaja existing-laatija)
       :laatija (:id existing-laatija)})
    (let [kayttaja-id (kayttaja-service/add-kayttaja! db kayttaja)]
      {:kayttaja kayttaja-id
       :laatija (laatija-db/insert-laatija<! db (assoc laatija :kayttaja kayttaja-id))})))

(defn upsert-kayttaja-laatijat! [db kayttaja-laatijat]
  (jdbc/with-db-transaction
    [db db]
    (mapv #(upsert-kayttaja-laatija! db %) kayttaja-laatijat)))

(defn find-laatija-yritykset [db id]
  (map :yritys-id (laatija-db/select-laatija-yritykset db {:id id})))

(defn attach-laatija-yritys [db laatija-id yritys-id]
  (laatija-db/insert-laatija-yritys! db (map/bindings->map laatija-id yritys-id)))

(defn attach-laatija-yritys [db laatija-id yritys-id]
  (laatija-db/insert-laatija-yritys! db (map/bindings->map laatija-id yritys-id)))

(defn detach-laatija-yritys [db laatija-id yritys-id]
  (laatija-db/delete-laatija-yritys! db (map/bindings->map laatija-id yritys-id)))

;;
;; Pätevyydet
;;

(def patevyystasot [{:id 1 :label-fi "Perustaso" :label-sv "Basnivå"}
                    {:id 2 :label-fi "Ylempi taso" :label-sv "Högre nivå"}])

(defn find-patevyystasot [] patevyystasot)

