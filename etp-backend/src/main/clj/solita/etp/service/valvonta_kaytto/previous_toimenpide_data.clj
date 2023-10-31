(ns solita.etp.service.valvonta-kaytto.previous-toimenpide-data
  (:require [solita.common.time :as time]
            [solita.etp.db :as db]
            [solita.etp.service.valvonta-kaytto.toimenpide :as toimenpide])
  (:import (java.time LocalDate)))

(db/require-queries 'previous-toimenpide-data)

(defmulti previous-toimenpide-data
          (fn [_db toimenpide _valvonta-id] (-> toimenpide :type-id toimenpide/type-key)))

(defn format-date-values [data]
  (update-vals data (fn [value]
                        (if (= (type value) LocalDate)
                          (time/format-date value)
                          value))))

(defmethod previous-toimenpide-data :penalty-decision-hearing-letter [db _toimenpide valvonta-id]
  (let [values (first (previous-toimenpide-data-db/sakkopaatos-kuulemiskirje db {:valvonta-id valvonta-id}))]
    (format-date-values values)))

(defmethod previous-toimenpide-data :penalty-decision-actual-decision [db _toimenpide valvonta-id]
  ;; TODO: Olisiko selvempää muodostaa palautettava mappi tässä
  (let [values (first (previous-toimenpide-data-db/sakkopaatos-varsinainen-paatos db {:valvonta-id valvonta-id}))]
    (format-date-values values)))

(defmethod previous-toimenpide-data :default [_ _toimenpide _valvonta-id]
  {})