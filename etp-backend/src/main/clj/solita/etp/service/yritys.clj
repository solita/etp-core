(ns solita.etp.service.yritys
  (:require [solita.etp.db :as db]
            [solita.etp.schema.yritys :as yritys-schema]
            [schema.coerce :as coerce]
            [solita.etp.exception :as exception]
            [solita.etp.service.laatija-yritys :as laatija-yritys]
            [solita.etp.service.rooli :as rooli-service]
            [solita.common.map :as map]))

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

(defn find-laatijat [db id]
  (yritys-db/select-laatijat db {:id id}))

(defn find-all-laskutuskielet [db]
  (yritys-db/select-all-laskutuskielet db))

(defn find-all-verkkolaskuoperaattorit [db]
  (yritys-db/select-all-verkkolaskuoperaattorit db))

(defn laatija-in-yritys? [db laatija-id yritys-id]
  (some #(= laatija-id(:id %))
        (filter laatija-yritys/accepted? (find-laatijat db yritys-id))))

(defn add-laatija-yritys! [db whoami laatija-id yritys-id]
  (if (or (laatija-in-yritys? db (:id whoami) yritys-id)
          (rooli-service/paakayttaja? whoami))
    (do
      (yritys-db/insert-laatija-yritys!
        db
        (map/bindings->map laatija-id yritys-id))
      nil)
    (exception/throw-forbidden!
      (str "User " (:id whoami) " does not belong to organization: " yritys-id))))
