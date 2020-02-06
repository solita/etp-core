(ns solita.etp.test-system
  (:require [integrant.core :as ig]
            [clojure.java.jdbc :as jdbc]
            [solita.etp.db]))

(def ^:dynamic *db* nil)

(def config-file-path "src/main/resources/config-for-tests.edn")

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
        config (assoc-in (-> config-file-path slurp ig/read-string)
                         [[:solita.etp/db :db/etp-app] :database-name]
                         db-name)
        management-system (ig/init config [[:solita.etp/db :db/etp]])
        _ (create-db! (get management-system [:solita.etp/db :db/etp]) db-name)
        system (ig/init config [[:solita.etp/db :db/etp-app]])
        ]
    (with-bindings {#'*db* (get system [:solita.etp/db :db/etp-app])}
      (f))
    (ig/halt! system)
    (drop-db! (get management-system [:solita.etp/db :db/etp]) db-name)
    (ig/halt! management-system)))
