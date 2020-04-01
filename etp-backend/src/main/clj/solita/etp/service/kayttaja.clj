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

(defn find-kayttaja [db logged-in-kayttaja id]
  (when (or (= id (:id logged-in-kayttaja))
            (rooli-service/more-than-laatija? logged-in-kayttaja))
    (->> {:id id}
         (kayttaja-db/select-kayttaja db)
         (map coerce-kayttaja)
         first)))

(defn find-kayttaja-with-email [db logged-in-kayttaja email]
  (when (or (= email (:email logged-in-kayttaja))
            (rooli-service/more-than-laatija? logged-in-kayttaja))
    (->> {:email email}
         (kayttaja-db/select-kayttaja-with-email db)
         (map coerce-kayttaja)
         first)))

(defn update-login! [db id cognitoid]
  (kayttaja-db/update-login! db {:id id :cognitoid cognitoid}))

(defn add-kayttaja!
  ([db kayttaja]
   (:id (kayttaja-db/insert-kayttaja<! db kayttaja)))
  ([db logged-in-kayttaja kayttaja]
   (when (rooli-service/paakayttaja? logged-in-kayttaja)
     (add-kayttaja! db kayttaja))))

(defn update-kayttaja!
  ([db id kayttaja]
   (kayttaja-db/update-kayttaja! db (assoc kayttaja :id id)))
  ([db logged-in-kayttaja id kayttaja]
   (when (or (= id (:id logged-in-kayttaja))
             (rooli-service/paakayttaja? logged-in-kayttaja))
     (update-kayttaja! db id kayttaja))))
