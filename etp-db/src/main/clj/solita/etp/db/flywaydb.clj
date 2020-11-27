(ns solita.etp.db.flywaydb
  (:require [clojure.string :as str])
  (:import (org.flywaydb.core Flyway)
           (org.flywaydb.core.api.configuration FluentConfiguration)
           (org.flywaydb.core.internal.configuration ConfigUtils)))

(def guide
  (str/join
    \newline
    ["Usage: java -jar etp-db.jar [command]"
     "Parameters: DB_URL, DB_USER and DB_PASSWORD can be defined as environment variables. Otherwise common default values are used. "
     "Supported commands are: "
     "- clean"
     "- migrate"]))

(def flyway-configuration {
   ConfigUtils/SCHEMAS                 "etp,audit"
   ConfigUtils/SQL_MIGRATION_PREFIX    "v"
   ConfigUtils/SQL_MIGRATION_SEPARATOR "-"
   ConfigUtils/REPEATABLE_SQL_MIGRATION_PREFIX "r"
   ConfigUtils/LOCATIONS "classpath:migration"})

(defn map-keys [f m] (into {} (map (fn [[k, v]] [(f k) v]) m)))

(defn configure-flyway [db]
  (-> ^FluentConfiguration (Flyway/configure)
      (.dataSource (:url db) (:user db) (:password db))
      (.configuration flyway-configuration)
      .load))

(defn env [name default]
  (or (System/getenv name) default))

(defn read-configuration []
  {:user (env "DB_USER" "etp")
   :password (env "DB_PASSWORD" "etp")
   :url (env "DB_URL" "jdbc:postgresql://localhost:5432/postgres")})

(defn run [args]
  (let [command (str/trim (or (first args) "<empty string>"))
        db (read-configuration)
        flyway (configure-flyway db)]
    (case command
      "clean" (.clean flyway)
      "migrate" (.migrate flyway)
      (do
        (println "Unsupported command:" command)
        (println guide)))))

(defn -main [& args] (run args))
