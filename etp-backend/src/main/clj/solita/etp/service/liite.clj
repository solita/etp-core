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
  (db/with-db-exception-translation jdbc/insert! [db :liite liite db/default-opts]))

(defn- file-key [liite-id]
  (str "energiatodistus/liite/" liite-id))

(defn- insert-file! [key aws-s3-client file]
  (file-service/upsert-file-from-file aws-s3-client key file))

(defn add-liite-from-file! [db aws-s3-client whoami energiatodistus-id liite]
  (jdbc/with-db-transaction [db db]
    (-> liite
        (dissoc :tempfile :size)
        (assoc :createdby-id (:id whoami))
        (assoc :energiatodistus-id energiatodistus-id)
        (insert-liite! db)
        first
        :id
        file-key
        (insert-file! aws-s3-client (:tempfile liite)))))

(defn add-liitteet-from-files [db aws-s3-client whoami energiatodistus-id files]
  (println aws-s3-client)
  (jdbc/with-db-transaction [db db]
    (doseq [file files]
      (add-liite-from-file! db aws-s3-client whoami energiatodistus-id
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

(defn find-energiatodistus-liite-content [db aws-s3-client liite-id]
  (merge
    (file-service/find-file aws-s3-client (file-key liite-id))
    (first (liite-db/select-liite db {:id liite-id}))))

(defn delete-liite [db liite-id]
  (liite-db/delete-liite! db {:id liite-id}))
