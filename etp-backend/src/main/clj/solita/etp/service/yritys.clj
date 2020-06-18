(ns solita.etp.service.yritys
  (:require [solita.etp.db :as db]
            [solita.etp.schema.yritys :as yritys-schema]
            [schema.coerce :as coerce]))

; *** Require sql functions ***
(db/require-queries 'yritys)

(defn add-yritys! [db yritys]
  (:id (yritys-db/insert-yritys<! db yritys)))

(defn update-yritys! [db id yritys]
  (yritys-db/update-yritys! db (assoc yritys :id id)))

(defn find-yritys [db id]
  (first (yritys-db/select-yritys db {:id id})))

(defn find-all-yritykset [db]
  (yritys-db/select-all-yritykset db))

(defn find-all-verkkolaskuoperaattorit [db]
  (yritys-db/select-all-verkkolaskuoperaattorit db))
