(ns solita.etp.test-system
  (:require [integrant.core :as ig]
            [clojure.java.jdbc :as jdbc]
            [solita.etp.config :as config]
            [solita.etp.db]))

(def ^:dynamic *db* nil)

(defn config-for-management []
  (config/db (config/env "DB_HOST" "localhost")
             (config/env "DB_PORT" 5432)
             (config/env "DB_MANAGEMENT_USER" "etp")
             (config/env "DB_MANAGEMENT_PASSWORD" "etp")
             (config/env "DB_DATABASE" "postgres")
             (config/env "DB_SCHEMA" "etp")))

(defn config-for-tests [db-name]
  (config/db (config/env "DB_HOST" "localhost")
             (config/env "DB_PORT" 5432)
             (config/env "DB_USER" "etp_app")
             (config/env "DB_PASSWORD" "etp")
             (config/env "DB_SCHEMA" "etp")
             db-name))


(def db-name-counter (atom 0))

(defn next-db-name []
  (str "etp_test_" (first (swap-vals! db-name-counter inc))))

(defn create-db! [db db-name]
  (jdbc/execute! db
                 [(format "CREATE DATABASE %s TEMPLATE postgres" db-name)]
                 {:transaction? false}))

(defn drop-db! [db db-name]
  (jdbc/execute! db
                 [(format "DROP DATABASE IF EXISTS %s" db-name)]
                 {:transaction? false}))

(defn fixture [f]
  (let [db-name (next-db-name)
        management-system (ig/init (config-for-management))
        management-db (:solita.etp/db management-system)
        _ (create-db! management-db db-name)
        test-system (ig/init (config-for-tests db-name))]
    (with-bindings {#'*db* (:solita.etp/db test-system)}
      (f))
    (ig/halt! test-system)
    (drop-db! management-db db-name)
    (ig/halt! management-system)))
