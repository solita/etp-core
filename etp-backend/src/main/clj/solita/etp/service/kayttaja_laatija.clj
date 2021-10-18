(ns solita.etp.service.kayttaja-laatija
  (:require [clojure.java.jdbc :as jdbc]
            [schema-tools.core :as st]
            [solita.etp.db :as db]
            [solita.etp.exception :as exception]
            [solita.etp.service.kayttaja :as kayttaja-service]
            [solita.etp.service.laatija :as laatija-service]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.schema.laatija :as laatija-schema]
            [solita.etp.schema.common :as common-schema]
            [flathead.flatten :as flat]))

(defn- update-kayttaja [db id kayttaja]
  (db/with-db-exception-translation
    jdbc/update!
    db
    :kayttaja
    (flat/tree->flat "$" (dissoc kayttaja :rooli))
    ["rooli_id = 0 and id = ?" id]))

(defn- add-kayttaja [db kayttaja]
  (kayttaja-service/add-kayttaja! db (assoc kayttaja :rooli 0)))

(defn- upsert-kayttaja-laatija! [db {:keys [henkilotunnus] :as kayttaja-laatija}]
  "Upserts käyttäjä and laatija WITHOUT transaction."
  (let [kayttaja (st/select-schema kayttaja-laatija laatija-schema/KayttajaAdd)
        laatija (st/select-schema kayttaja-laatija laatija-schema/LaatijaAdd)
        existing-laatija (laatija-service/find-laatija-by-henkilotunnus db henkilotunnus)
        id (:id existing-laatija)]
    (if existing-laatija
      (do
        (update-kayttaja db id (dissoc kayttaja :henkilotunnus))
        (laatija-service/update-laatija-by-id! db id laatija)
        id)
      (let [id (add-kayttaja db kayttaja)]
        (laatija-service/add-laatija! db (assoc laatija :id id))))))

(defn upsert-kayttaja-laatijat! [db kayttaja-laatijat]
  (jdbc/with-db-transaction
    [db db]
    (mapv #(upsert-kayttaja-laatija! db %) kayttaja-laatijat)))

(defn update-kayttaja-laatija! [db whoami id kayttaja-laatija]
  (if (or (and (= id (:id whoami))
               (common-schema/not-contains-keys
                kayttaja-laatija laatija-schema/LaatijaAdminUpdate)
               (common-schema/not-contains-keys
                kayttaja-laatija laatija-schema/KayttajaAdminUpdate))
          (rooli-service/paakayttaja? whoami))
    (let [kayttaja (st/select-schema kayttaja-laatija laatija-schema/KayttajaUpdate)
          laatija (st/select-schema kayttaja-laatija laatija-schema/LaatijaUpdate)]
      (jdbc/with-db-transaction
        [db db]
        (update-kayttaja db id kayttaja)
        (laatija-service/update-laatija-by-id! db id laatija)))
    (exception/throw-forbidden!)))
