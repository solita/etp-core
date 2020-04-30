(ns solita.etp.service.liite
  (:require [solita.etp.db :as db]
            [solita.etp.schema.liite :as liite-schema]
            [solita.etp.service.json :as json]
            [solita.etp.service.file :as file-service]
            [schema.coerce :as coerce]
            [clojure.java.jdbc :as jdbc]
            [clojure.set :as set]))

; *** Require sql functions ***
(db/require-queries 'liite)

; *** Conversions from database data types ***
(def coerce-liite (coerce/coercer liite-schema/Liite json/json-coercions))

(defn- insert-liite! [liite db]
  (db/with-db-exception-translation jdbc/insert! [db :liite liite]))

(defn- file-key [liite-id]
  (str "energiatodistus/liite/" liite-id))

(defn- insert-file! [key db file]
  (file-service/upsert-file-from-file db key file))

(defn add-liite-from-file! [db whoami energiatodistus-id liite]
  (jdbc/with-db-transaction [db db]
    (-> liite
        (dissoc :tempfile :size)
        (assoc :createdby_id (:id whoami))
        (assoc :energiatodistus_id energiatodistus-id)
        (insert-liite! db)
        first
        :id
        file-key
        (insert-file! db (:tempfile liite)))))

(defn add-liitteet-from-files [db whoami energiatodistus-id files]
  (jdbc/with-db-transaction [db db]
    (doseq [file files]
      (add-liite-from-file! db whoami energiatodistus-id
        (set/rename-keys file {:content-type :contenttype
                               :filename :nimi})))))

(defn find-energiatodistus-liitteet [db energiatodistus-id]
  (map coerce-liite
       (liite-db/select-liite-by-energiatodistus-id
         db {:energiatodistus-id energiatodistus-id})))
