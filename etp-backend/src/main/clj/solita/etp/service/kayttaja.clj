(ns solita.etp.service.kayttaja
  (:require [schema.coerce :as coerce]
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
   (when (or (= id (:id whoami))
             (rooli-service/more-than-laatija? whoami))
     (find-kayttaja db id))))

(defn update-login! [db id cognitoid]
  (kayttaja-db/update-login! db {:id id :cognitoid cognitoid}))

(defn add-kayttaja!
  ([db kayttaja]
   (:id (kayttaja-db/insert-kayttaja<! db kayttaja)))
  ([db whoami kayttaja]
   (when (rooli-service/paakayttaja? whoami)
     (add-kayttaja! db kayttaja))))

(defn update-kayttaja!
  ([db id kayttaja]
   (kayttaja-db/update-kayttaja! db (assoc kayttaja :id id)))
  ([db whoami id kayttaja]
   (when (or (= id (:id whoami))
             (rooli-service/paakayttaja? whoami))
     (update-kayttaja! db id kayttaja))))
