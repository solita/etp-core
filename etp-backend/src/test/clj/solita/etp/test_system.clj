(ns solita.etp.test-system
  (:require [integrant.core :as ig]
            [clojure.java.jdbc :as jdbc]
            [solita.etp.db]))

(def ^:dynamic *db* nil)

(def config-file-path "src/main/resources/config-for-tests.edn")

(def db-names (for [i (range 5500 5600)] (str "etp_test_" i)))

(defonce free-db-names (atom (apply sorted-set db-names)))

(defn reserve-db-name []
  (ffirst (swap-vals! free-db-names #(->> % rest (apply sorted-set)))))

(defn release-db-name [s]
  (swap! free-db-names conj s)
  s)

(defn create-db! [db db-name]
  (jdbc/execute! db
                 [(format "CREATE DATABASE %s TEMPLATE postgres" db-name)]
                 {:transaction? false}))

(defn drop-db! [db db-name]
  (jdbc/execute! db
                 [(format "DROP DATABASE IF EXISTS %s" db-name)]
                 {:transaction? false}))

(defn fixture [f]
  (let [db-name (reserve-db-name)
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
