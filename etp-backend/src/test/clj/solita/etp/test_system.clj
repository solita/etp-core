(ns solita.etp.test-system
  (:require [integrant.core :as ig]
            [clojure.java.jdbc :as jdbc]
            [solita.common.jdbc :as common-jdbc]
            [solita.etp.config :as config]
            [solita.etp.db]
            [solita.etp.aws-s3-client]
            [solita.common.aws :as aws]))

(def ^:dynamic *db* nil)
(def ^:dynamic *aws-s3-client* nil)

(defn config-for-management []
  (merge (config/db {:username       (config/env "DB_MANAGEMENT_USER" "etp")
                     :password       (config/env "DB_MANAGEMENT_PASSWORD" "etp")
                     :database-name  "template1"
                     :current-schema "public"})
         (config/aws-s3-client)))

(defn config-for-tests [db-name]
  (merge (config/db {:database-name            db-name
                     :re-write-batched-inserts true})
         (config/aws-s3-client)))

(def db-name-counter (atom 0))

(defn next-db-name []
  (str "etp_test_" (first (swap-vals! db-name-counter inc))))

(defn next-bucket-name []
  (str "etp-test-" (first (swap-vals! db-name-counter inc))))

(defn create-db! [db db-name]
  (jdbc/execute! db
                 [(format "CREATE DATABASE %s TEMPLATE postgres" db-name)]
                 {:transaction? false}))

(defn drop-db! [db db-name]
  (jdbc/execute! db
                 [(format "DROP DATABASE IF EXISTS %s" db-name)]
                 {:transaction? false}))

(defn create-bucket! [aws-s3-client bucket]
  (#'aws/invoke aws-s3-client :CreateBucket {:Bucket bucket}))

(defn drop-bucket! [aws-s3-client bucket]
  (#'aws/invoke aws-s3-client :DeleteBucket {:Bucket bucket}))

(defn fixture [f]
  (let [db-name                  (next-db-name)
        bucket-name              (next-bucket-name)
        management-system        (ig/init (config-for-management))
        management-db            (:solita.etp/db management-system)
        _                        (create-db! management-db db-name)
        test-system              (ig/init (config-for-tests db-name))
        management-aws-s3-client (:solita.etp/aws-s3-client management-system)
        _                        (create-bucket! management-aws-s3-client bucket-name)]
    (with-bindings {#'*db*            (:solita.etp/db test-system)
                    #'*aws-s3-client* (:solita.etp/aws-s3-client test-system)}
      (with-redefs [config/getFilesBucketName (fn [] bucket-name)]
        (common-jdbc/with-application-name-support f)))

    (ig/halt! test-system)
    (drop-db! management-db db-name)
    (drop-bucket! management-aws-s3-client bucket-name)
    (ig/halt! management-system)))

(defn db-user
  ([kayttaja-id] (db-user *db* kayttaja-id))
  ([db kayttaja-id]
    (assoc db :application-name (str kayttaja-id "@core.etp.test"))))