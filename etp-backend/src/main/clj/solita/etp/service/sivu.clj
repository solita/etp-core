(ns solita.etp.service.sivu
  (:require [clojure.java.jdbc :as jdbc]
            [solita.etp.db :as db]
            [solita.etp.service.rooli :as rooli-service]))

(db/require-queries 'sivu)

(defn find-all-sivut [db whoami]
  (sivu-db/select-all-sivut db {:paakayttaja (rooli-service/paakayttaja? whoami)}))

(defn find-sivu [db whoami id]
  (first (sivu-db/select-sivu db {:id id
                                  :paakayttaja (rooli-service/paakayttaja? whoami)})))

(defn add-sivu! [db sivu]
  (sivu-db/insert-sivu<! db sivu))

(defn update-sivu! [db id sivu]
  (first (db/with-db-exception-translation
           jdbc/update! db :sivu sivu ["id = ?" id] db/default-opts)))
