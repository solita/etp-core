(ns solita.etp.service.kayttaja
  (:require [clojure.java.jdbc :as jdbc]
            [schema.coerce :as coerce]
            [solita.etp.exception :as exception]
            [solita.etp.db :as db]
            [solita.etp.service.json :as json]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.schema.kayttaja :as kayttaja-schema]
            [solita.etp.schema.common :as common-schema]
            [flathead.flatten :as flat]))

;; *** Require sql functions ***
(db/require-queries 'kayttaja)

;; *** Conversions from database data types ***
(def coerce-kayttaja (coerce/coercer kayttaja-schema/Kayttaja json/json-coercions))

(defn find-kayttaja
  ([db id]
   (->> {:id id}
        (kayttaja-db/select-kayttaja db)
        (map (partial flat/flat->tree #"\$"))
        (map coerce-kayttaja)
        first))
  ([db whoami id]
   (when-let [kayttaja (find-kayttaja db id)]
     (if (or (= id (:id whoami))
             (rooli-service/paakayttaja? whoami)
             (and (rooli-service/patevyydentoteaja? whoami)
                  (rooli-service/laatija? kayttaja)))
       kayttaja
       (exception/throw-forbidden!)))))

(defn add-kayttaja! [db kayttaja]
  (-> (jdbc/insert! db :kayttaja (flat/tree->flat "$" kayttaja)) first :id))

(defn update-kayttaja! [db whoami id kayttaja]
  (if (or (and (= id (:id whoami))
               (common-schema/not-contains-keys
                kayttaja
                kayttaja-schema/KayttajaAdminUpdate))
          (rooli-service/paakayttaja? whoami))
    (jdbc/update! db :kayttaja (flat/tree->flat "$" kayttaja) ["rooli <> 0 and id = ?" id])
    (exception/throw-forbidden!)))
