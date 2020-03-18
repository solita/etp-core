(ns solita.etp.service.kayttaja
  (:require [clojure.java.jdbc :as jdbc]
            [schema.coerce :as coerce]
            [solita.etp.db :as db]
            [solita.etp.service.json :as json]
            [solita.etp.schema.kayttaja :as kayttaja-schema]))

; *** Require sql functions ***
(db/require-queries 'kayttaja)

; *** Conversions from database data types ***
(def coerce-kayttaja (coerce/coercer kayttaja-schema/Kayttaja json/json-coercions))

(defn add-kayttaja! [db kayttaja]
  (:id (kayttaja-db/insert-kayttaja<! db kayttaja)))

(defn find-kayttaja [db id]
  (map coerce-kayttaja (kayttaja-db/select-kayttaja db {:id id})))
