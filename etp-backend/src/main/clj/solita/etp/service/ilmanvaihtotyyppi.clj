(ns solita.etp.service.ilmanvaihtotyyppi
  (:require [solita.etp.db :as db]))

;; *** Require sql functions ***
(db/require-queries 'ilmanvaihtotyyppi)

(defn find-ilmanvaihtotyypit [db]
  (ilmanvaihtotyyppi-db/select-ilmanvaihtotyypit db))
