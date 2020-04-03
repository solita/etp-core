(ns solita.etp.service.kayttaja-laatija
  (:require [clojure.java.jdbc :as jdbc]
            [schema.coerce :as coerce]
            [schema-tools.core :as st]
            [solita.etp.db :as db]
            [solita.etp.service.json :as json]
            [solita.etp.service.kayttaja :as kayttaja-service]
            [solita.etp.service.laatija :as laatija-service]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.schema.kayttaja-laatija :as kayttaja-laatija-schema]
            [solita.etp.schema.kayttaja :as kayttaja-schema]
            [solita.etp.schema.laatija :as laatija-schema]))

;; *** Require sql functions ***
(db/require-queries 'kayttaja-laatija)

;; *** Conversions from database data types ***
(def coerce-whoami (coerce/coercer kayttaja-laatija-schema/Whoami
                                   json/json-coercions))

(defn find-whoami [db email]
  (->> {:email email}
       (kayttaja-laatija-db/select-whoami db)
       (map coerce-whoami)
       first))

(defn- upsert-kayttaja-laatija! [db {:keys [henkilotunnus] :as kayttaja-laatija}]
  "Upserts käyttäjä and laatija WITHOUT transaction."
  (let [kayttaja (st/select-schema kayttaja-laatija kayttaja-schema/KayttajaAdd)
        laatija (st/select-schema kayttaja-laatija laatija-schema/LaatijaAdd)
        existing-laatija (laatija-service/find-laatija-with-henkilotunnus db henkilotunnus)]
    (if existing-laatija
      (do
        (laatija-service/update-laatija-with-kayttaja-id! db
                                                          (:kayttaja existing-laatija)
                                                          (merge existing-laatija laatija))
        {:kayttaja (:kayttaja existing-laatija)
         :laatija (:id existing-laatija)})
      (let [kayttaja-id (kayttaja-service/add-kayttaja! db kayttaja)]
        {:kayttaja kayttaja-id
         :laatija (laatija-service/add-laatija! db (assoc laatija :kayttaja kayttaja-id))}))))

(defn upsert-kayttaja-laatijat! [db kayttaja-laatijat]
  (jdbc/with-db-transaction
    [db db]
    (mapv #(upsert-kayttaja-laatija! db %) kayttaja-laatijat)))

(def ks-only-for-paakayttaja [:passivoitu :rooli :patevyystaso :toteamispaivamaara
                              :toteaja :laatimiskielto])

;; TODO should throw exception if ks-only-for-paakayttaja is used when
;; whoami is not paakayttaja
(defn update-kayttaja-laatija! [db whoami id kayttaja-laatija]
  (when (or (and (= id (:id whoami))
                 (every? #(not (contains? kayttaja-laatija %))
                         ks-only-for-paakayttaja))
            (rooli-service/paakayttaja? whoami))
    (let [kayttaja (st/select-schema kayttaja-laatija kayttaja-schema/KayttajaUpdate)
          laatija (st/select-schema kayttaja-laatija laatija-schema/LaatijaUpdate)]
      (jdbc/with-db-transaction
        [db db]
        (kayttaja-service/update-kayttaja! db id kayttaja)
        (laatija-service/update-laatija-with-kayttaja-id! db id laatija)))))
