(ns solita.etp.service.yritys
  (:require [solita.etp.db :as db]
            [solita.etp.schema.yritys :as yritys-schema]
            [solita.etp.service.json :as json]
            [schema.coerce :as coerce]))

; *** Require sql functions ***
(db/require-queries 'yritys)

; *** Conversions from database data types ***
(def coerce-yritys (coerce/coercer yritys-schema/Yritys coerce/+json-coercions+))

(defn add-yritys! [db yritys]
  (yritys-db/insert-yritys! db {:data (json/write-value-as-string yritys)}))

(defn merge-data [row]
  (let [data (-> row :data .getValue json/read-value)]
    (merge data (dissoc row :data))))

(defn find-yritys [db id]
  (first (map (comp coerce-yritys merge-data) (yritys-db/select-yritys db {:id id}))))