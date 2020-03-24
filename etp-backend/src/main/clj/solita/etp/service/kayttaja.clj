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
