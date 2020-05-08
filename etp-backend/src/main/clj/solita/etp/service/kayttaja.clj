(ns solita.etp.service.kayttaja
  (:require [clojure.java.jdbc :as jdbc]
            [schema.coerce :as coerce]
            [solita.etp.exception :as exception]
            [solita.etp.db :as db]
            [solita.etp.service.json :as json]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.schema.kayttaja :as kayttaja-schema]
            [solita.etp.schema.common :as common-schema]))

;; *** Require sql functions ***
(db/require-queries 'kayttaja)

;; *** Conversions from database data types ***
(def coerce-kayttaja (coerce/coercer kayttaja-schema/Kayttaja json/json-coercions))

;; *** Conversions from database data types ***
(def coerce-whoami (coerce/coercer kayttaja-schema/Whoami
                                   json/json-coercions))

(defn find-whoami
  ([db email]
   (find-whoami db email nil))
  ([db email cognitoid]
   (->> {:email email :cognitoid cognitoid}
        (kayttaja-db/select-whoami db)
        (map coerce-whoami)
        first)))

(defn find-kayttaja
  ([db id]
   (->> {:id id}
        (kayttaja-db/select-kayttaja db)
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
  (-> (jdbc/insert! db :kayttaja kayttaja) first :id))

(defn allow-rooli-update? [existing-rooli new-rooli]
  (or (nil? new-rooli)
      (= existing-rooli new-rooli)
      (and (not= existing-rooli 0)
           (not= new-rooli 0))))

(defn update-kayttaja! [db id whoami kayttaja]
  (if (or (and (= id (:id whoami))
               (common-schema/not-contains-keys
                 kayttaja kayttaja-schema/KayttajaAdminUpdate))
        (rooli-service/paakayttaja? whoami))
    (jdbc/update! db :kayttaja kayttaja ["rooli <> 0 and id = ?" id])
    (exception/throw-forbidden!)))

(defn update-kayttaja-with-whoami! [db whoami]
  (kayttaja-db/update-kayttaja-with-whoami! db whoami))
