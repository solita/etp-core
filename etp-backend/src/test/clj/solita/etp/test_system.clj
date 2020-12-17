(ns solita.etp.test-system
  (:require [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [integrant.core :as ig]
            [solita.common.jdbc :as common-jdbc]
            [solita.etp.config :as config]
            [solita.etp.db]
            [solita.etp.aws-s3-client]
            [solita.common.aws :as aws]))

(def ^:dynamic *db* nil)
(def ^:dynamic *aws-s3-client* nil)

(defn db-user
  ([kayttaja-id] (db-user *db* kayttaja-id))
  ([db kayttaja-id]
   (assoc db :application-name (str kayttaja-id "@core.etp.test"))))

(defn config-for-management [bucket]
  (merge (config/db {:username       (config/env "DB_MANAGEMENT_USER" "etp")
                     :password       (config/env "DB_MANAGEMENT_PASSWORD" "etp")
                     :database-name  "template1"
                     :current-schema "public"})
         (config/aws-s3-client {:bucket bucket})))

(defn config-for-tests [db-name bucket]
  (merge (config/db {:database-name            db-name
                     :re-write-batched-inserts true})
         (config/aws-s3-client {:bucket bucket})))

(defn create-db! [db db-name]
  (jdbc/execute! (db-user db "admin")
                 [(format "CREATE DATABASE %s TEMPLATE postgres" db-name)]
                 {:transaction? false}))

(defn drop-db! [db db-name]
  (jdbc/execute! (db-user db "admin")
                 [(format "DROP DATABASE IF EXISTS %s" db-name)]
                 {:transaction? false}))

(defn create-bucket! [{:keys [client bucket]}]
  (#'aws/invoke client :CreateBucket {:Bucket bucket
                                      :CreateBucketConfiguration
                                      {:LocationConstraint "eu-central-1"}}))

(defn drop-bucket! [{:keys [client bucket]}]
  (let [keys (->> (#'aws/invoke client :ListObjectsV2 {:Bucket bucket})
                  :Contents
                  (map #(select-keys % [:Key])))]
    (#'aws/invoke client :DeleteObjects {:Delete {:Objects keys}
                                         :Bucket bucket})
    (#'aws/invoke client :DeleteBucket {:Bucket bucket})))

(defn fixture [f]
  (let [uuid                     (-> (java.util.UUID/randomUUID)
                                     .toString
                                     (str/replace "-" ""))
        db-name                  (str "etp_test_" uuid)
        bucket-name              (str "files-" uuid)
        management-system        (ig/init (config-for-management bucket-name))
        management-db            (:solita.etp/db management-system)
        management-aws-s3-client (:solita.etp/aws-s3-client management-system)]
    (try
      (create-db! management-db db-name)
      (create-bucket! management-aws-s3-client)
      (let [test-system (ig/init (config-for-tests db-name bucket-name))]
        (with-bindings
          {#'*db*            (db-user (:solita.etp/db test-system) 0)
           #'*aws-s3-client* (:solita.etp/aws-s3-client test-system)}
          (try (f)
               (finally (ig/halt! test-system)))))
      (finally
        (drop-db! management-db db-name)
        (drop-bucket! management-aws-s3-client)
        (ig/halt! management-system)))))

