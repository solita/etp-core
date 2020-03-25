(ns solita.etp.service.kayttaja
  (:require [schema.coerce :as coerce]
            [solita.etp.db :as db]
            [solita.etp.service.json :as json]
            [solita.etp.schema.kayttaja :as kayttaja-schema]))

;; *** Require sql functions ***
(db/require-queries 'kayttaja)

;; *** Conversions from database data types ***
(def coerce-kayttaja (coerce/coercer kayttaja-schema/Kayttaja json/json-coercions))

(defn find-kayttaja [db id]
  (->> {:id id}
       (kayttaja-db/select-kayttaja db)
       (map coerce-kayttaja)
       first))

(defn add-kayttaja! [db kayttaja]
  (:id (kayttaja-db/insert-kayttaja<! db kayttaja)))

(defn update-kayttaja! [db id kayttaja]
  (kayttaja-db/update-kayttaja! db (assoc kayttaja :id id)))

(def roolit [{:id 0
              :label-fi "Laatija"
              :label-sv "Laatija-SV"}
             {:id 1
              :label-fi "Pätevyyden toteaja"
              :label-sv "Pätevyyden toteaja -SV"}
             {:id 2
              :label-fi "Pääkäyttäjä"
              :label-sv "Pääkäyttäjä-SV"}])

(defn find-roolit [] roolit)
