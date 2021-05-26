(ns solita.common.time
  (:import (java.time ZoneId LocalDate)
           (java.time.format DateTimeFormatter)))

(def timezone (ZoneId/of "Europe/Helsinki"))
(def date-formatter (.withZone (DateTimeFormatter/ofPattern "dd.MM.yyyy") timezone))

(defn today []
  (.format date-formatter (LocalDate/now)))