(ns solita.etp.service.yritys
  (:require [solita.etp.db :as db]
            [solita.etp.schema.yritys :as yritys-schema]
            [solita.etp.service.json :as json]
            [schema.coerce :as coerce]))

; *** Require sql functions ***
(db/require-queries 'yritys)

; *** Conversions from database data types ***
(def coerce-yritys (coerce/coercer yritys-schema/Yritys json/json-coercions))

(defn add-yritys! [db yritys]
  (:id (yritys-db/insert-yritys<! db {:data (json/write-value-as-string yritys)})))

(defn update-yritys! [db id yritys]
  (yritys-db/update-yritys! db {:id id :data (json/write-value-as-string yritys)}))

(defn find-yritys [db id]
  (first (map (comp coerce-yritys json/merge-data) (yritys-db/select-yritys db {:id id}))))
