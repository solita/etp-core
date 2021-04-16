(ns solita.etp.service.valvonta
  (:require [solita.etp.db :as db]))

(db/require-queries 'valvonta)

(defn find-valvojat [db] (valvonta-db/select-valvojat db))

(defn find-valvonta [db id]
  (first (valvonta-db/select-valvonta db {:id id})))

(defn update-valvonta! [db id active?]
  (valvonta-db/update-valvonta! db {:id id :active? active?}))