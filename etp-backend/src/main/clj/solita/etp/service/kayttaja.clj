(ns solita.etp.service.kayttaja
  (:require [clojure.java.jdbc :as jdbc]
            [schema.coerce :as coerce]
            [solita.etp.db :as db]
            [solita.etp.service.json :as json]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.schema.kayttaja :as kayttaja-schema]))

;; *** Require sql functions ***
(db/require-queries 'kayttaja)

;; *** Conversions from database data types ***
(def coerce-kayttaja (coerce/coercer kayttaja-schema/Kayttaja json/json-coercions))

(defn find-kayttaja
  ([db id]
   (->> {:id id}
        (kayttaja-db/select-kayttaja db)
        (map coerce-kayttaja)
        first))
  ([db whoami id]
   (let [kayttaja (find-kayttaja db id)]
     (when (or (= id (:id whoami))
               (rooli-service/paakayttaja? whoami)
               (and (rooli-service/patevyydentoteaja? whoami)
                    (rooli-service/laatija? kayttaja)))
       kayttaja))))

(defn update-login! [db id cognitoid]
  (kayttaja-db/update-login! db {:id id :cognitoid cognitoid}))

(defn add-kayttaja! [db kayttaja]
  (-> (jdbc/insert! db :kayttaja kayttaja) first :id))

(defn update-kayttaja! [db id kayttaja]
  (jdbc/update! db :kayttaja kayttaja ["id = ?" id]))
