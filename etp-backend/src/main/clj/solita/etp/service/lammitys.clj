(ns solita.etp.service.lammitys
  (:require [solita.etp.db :as db]))

;; *** Require sql functions ***
(db/require-queries 'lammitys)

(defn find-lammitysmuodot [db]
  (lammitys-db/select-lammitysmuodot db))

(defn find-lammonjaot [db]
  (lammitys-db/select-lammonjaot db))
