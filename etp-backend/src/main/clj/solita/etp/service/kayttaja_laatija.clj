(ns solita.etp.service.kayttaja-laatija
  (:require [clojure.java.jdbc :as jdbc]
            [schema-tools.core :as st]
            [solita.etp.schema.kayttaja :as kayttaja-schema]
            [solita.etp.schema.laatija :as laatija-schema]
            [solita.etp.service.kayttaja :as kayttaja-service]
            [solita.etp.service.laatija :as laatija-service]))

(defn- upsert-kayttaja-laatija! [db {:keys [henkilotunnus] :as kayttaja-laatija}]
  "Upserts käyttäjä and laatija WITHOUT transaction."
  (let [kayttaja (st/select-schema kayttaja-laatija kayttaja-schema/KayttajaAdd)
        laatija (st/select-schema kayttaja-laatija laatija-schema/LaatijaAdd)
        existing-laatija (laatija-service/find-laatija-with-henkilotunnus db henkilotunnus)]
    (if existing-laatija
      (do
        (laatija-service/update-laatija! db (merge existing-laatija laatija))
        {:kayttaja (:kayttaja existing-laatija)
         :laatija (:id existing-laatija)})
      (let [kayttaja-id (kayttaja-service/add-kayttaja! db kayttaja)]
        {:kayttaja kayttaja-id
         :laatija (laatija-service/add-laatija! db (assoc laatija :kayttaja kayttaja-id))}))))

(defn upsert-kayttaja-laatijat! [db kayttaja-laatijat]
  (jdbc/with-db-transaction
    [db db]
    (mapv #(upsert-kayttaja-laatija! db %) kayttaja-laatijat)))
