(ns solita.etp.system
  (:require [integrant.core :as ig]
            [solita.etp.config :as config]
            [solita.etp.db]
            [solita.etp.http-server]))

(defn config []
  (merge {}
         (config/db (config/env "DB_HOST" "localhost")
                    (config/env "DB_PORT" 5432)
                    (config/env "DB_USER" "etp_app")
                    (config/env "DB_PASSWORD" "etp")
                    (config/env "DB_DATABASE" "etp_dev")
                    (config/env "DB_SCHEMA" "etp"))
         (config/http-server (config/env "HTTP_SERVER_PORT" 8080))))

(defn start! []
  (ig/init (config)))
