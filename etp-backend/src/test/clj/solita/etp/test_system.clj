(ns solita.etp.test-system
  (:require [integrant.core :as ig]
            [clojure.java.jdbc :as jdbc]
            [solita.etp.db]))

(def ^:dynamic *db* nil)

(def config-for-management {:solita.etp/db {:adapter "postgresql"
                                            :server-name "localhost"
                                            :username "etp"
                                            :password "etp"
                                            :database-name "postgres"}})

(def config-for-tests {:solita.etp/db {:adapter "postgresql"
                                       :server-name "localhost"
                                       :username "etp_app"
                                       :password "etp"}})

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
        management-system (ig/init config-for-management)
        management-db (:solita.etp/db management-system)
        _ (create-db! management-db db-name)
        config (assoc-in config-for-tests
                         [:solita.etp/db :database-name]
                         db-name)
        system (ig/init config)]
    (with-bindings {#'*db* (:solita.etp/db system)}
      (f))
    (ig/halt! system)
    (drop-db! management-db db-name)
    (ig/halt! management-system)))
