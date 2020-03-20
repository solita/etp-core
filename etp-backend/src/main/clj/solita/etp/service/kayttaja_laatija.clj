(ns solita.etp.service.kayttaja-laatija
  (:require [clojure.java.jdbc :as jdbc]
            [solita.etp.schema.laatija :as kayttaja-laatija-schema]
            [solita.etp.service.kayttaja :as kayttaja-service]
            [solita.etp.service.laatija :as laatija-service]))

;; TODO doing this with a join instead of two queries would be slighty more efficient
(defn find-kayttaja-laatija [db id]
  (jdbc/with-db-transaction
    [db db]
    (when-let [kayttaja (kayttaja-service/find-kayttaja db id)]
      {:kayttaja kayttaja
       :laatija (laatija-service/find-laatija-with-kayttaja-id db (:id kayttaja))})))

;; TODO doing this with a join instead of two queries would be slighty more efficient
(defn find-kayttaja-laatija-with-henkilotunnus [db henkilotunnus]
  (jdbc/with-db-transaction
    [db db]
    (when-let [laatija (laatija-service/find-laatija-with-henkilotunnus
                        db
                        henkilotunnus)]
      {:kayttaja (kayttaja-service/find-kayttaja db (:kayttaja laatija))
       :laatija laatija})))

(defn- upsert-kayttaja-laatija! [db {:keys [kayttaja laatija]}]
  "Upserts käyttäjä and laatija WITHOUT transaction."
  (if-let [existing-kayttaja-laatija (find-kayttaja-laatija-with-henkilotunnus
                                      db
                                      (:henkilotunnus laatija))]
    (let [existing-kayttaja (:kayttaja existing-kayttaja-laatija)
          existing-laatija (:laatija existing-kayttaja-laatija)]
      (laatija-service/update-laatija! db (-> existing-laatija (merge laatija)))
      {:kayttaja (:kayttaja existing-laatija)
       :laatija (:id existing-laatija)})
    (let [kayttaja-id (kayttaja-service/add-kayttaja! db kayttaja)]
      {:kayttaja kayttaja-id
       :laatija (laatija-service/add-laatija! db (assoc laatija :kayttaja kayttaja-id))})))

(defn upsert-kayttaja-laatijat! [db kayttaja-laatijat]
  (jdbc/with-db-transaction
    [db db]
    (mapv #(upsert-kayttaja-laatija! db %) kayttaja-laatijat)))
