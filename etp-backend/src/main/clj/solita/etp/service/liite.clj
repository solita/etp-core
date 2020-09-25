(ns solita.etp.service.liite
  (:require [solita.etp.db :as db]
            [solita.etp.schema.liite :as liite-schema]
            [solita.etp.service.json :as json]
            [solita.etp.service.file :as file-service]
            [schema.coerce :as coerce]
            [clojure.java.jdbc :as jdbc]
            [solita.common.aws :as aws]

            [clojure.set :as set]))

; *** Require sql functions ***
(db/require-queries 'liite)

; *** Conversions from database data types ***
(def coerce-liite (coerce/coercer liite-schema/Liite json/json-coercions))

(defn- insert-liite! [liite db]
  (db/with-db-exception-translation jdbc/insert! [db :liite liite db/default-opts]))

(defn- file-key [liite-id]
  (str "energiatodistus/liite/" liite-id))

(defn- insert-file! [key db file]
  (aws/put-object key (file-service/file->byte-array file)))

(defn add-liite-from-file! [db whoami energiatodistus-id liite]
  (jdbc/with-db-transaction [db db]
    (-> liite
        (dissoc :tempfile :size)
        (assoc :createdby-id (:id whoami))
        (assoc :energiatodistus-id energiatodistus-id)
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

(defn add-liite-from-link! [db whoami energiatodistus-id liite]
  (jdbc/with-db-transaction [db db]
    (-> liite
        (assoc :createdby-id (:id whoami))
        (assoc :energiatodistus-id energiatodistus-id)
        (assoc :contenttype "text/uri-list")
        (insert-liite! db))))

(defn find-energiatodistus-liitteet [db energiatodistus-id]
  (map coerce-liite
       (liite-db/select-liite-by-energiatodistus-id
         db {:energiatodistus-id energiatodistus-id})))

(defn find-energiatodistus-liite-content [db liite-id]
  (merge
    {:content (aws/get-object (file-key liite-id))}
    (first (liite-db/select-liite db {:id liite-id}))))

(defn delete-liite [db liite-id]
  (liite-db/delete-liite! db {:id liite-id}))
