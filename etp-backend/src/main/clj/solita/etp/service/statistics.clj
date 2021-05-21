(ns solita.etp.service.statistics
  (:require [solita.etp.db :as db]))

;; *** Require sql functions ***
(db/require-queries 'statistics)

(defn find-statistics [db query]
  {})
