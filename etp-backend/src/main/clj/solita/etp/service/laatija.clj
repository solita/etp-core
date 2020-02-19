(ns solita.etp.service.laatija
  (:require [solita.etp.db :as db]
            [solita.etp.schema.laatija :as laatija-schema]
            [solita.etp.service.json :as json]
            [schema.coerce :as coerce]))

; *** Require sql functions ***
(db/require-queries 'laatija)

; *** Conversions from database data types ***
(def coerce-laatija (coerce/coercer laatija-schema/Laatija coerce/+json-coercions+))

(defn add-laatija! [db laatija]
  (:id (laatija-db/insert-laatija<! db {:data (json/write-value-as-string laatija)})))

(defn find-laatija [db id]
  (first (map (comp coerce-laatija json/merge-data) (laatija-db/select-laatija db {:id id}))))

;;
;; Pätevyydet
;;

(def patevyydet [{:id 0 :label-fi "Perustaso" :label-swe "Basnivå"}
                 {:id 1 :label-fi "Ylempi taso" :label-swe "Högre nivå"}])

(defn find-patevyydet [] patevyydet)
