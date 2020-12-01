(ns solita.etp.service.yritys
  (:require [solita.etp.db :as db]
            [solita.etp.exception :as exception]
            [solita.etp.service.laatija-yritys :as laatija-yritys]
            [solita.etp.service.rooli :as rooli-service]
            [solita.common.map :as map]
            [clojure.java.jdbc :as jdbc]))

; *** Require sql functions ***
(db/require-queries 'yritys)

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
  (some #(= laatija-id (:id %))
        (filter laatija-yritys/accepted? (find-laatijat db yritys-id))))

(defn add-laatija-yritys!
  ([db whoami laatija-id yritys-id]
   (if (or (laatija-in-yritys? db (:id whoami) yritys-id)
           (rooli-service/paakayttaja? whoami))
     (add-laatija-yritys! db laatija-id yritys-id)
     (exception/throw-forbidden!
       (str "User " (:id whoami) " is not paakayttaja or "
            "does not belong to organization: " yritys-id))))
  ([db laatija-id yritys-id]
   (yritys-db/insert-laatija-yritys!
     db (map/bindings->map laatija-id yritys-id))
   nil))

(defn add-yritys!
  ([db yritys]
    (:id (yritys-db/insert-yritys<! db yritys)))
  ([db whoami yritys]
    (jdbc/with-db-transaction [db db]
      (let [id (add-yritys! db yritys)]
        (add-laatija-yritys! db (:id whoami) id)
        id))))