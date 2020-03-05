(ns solita.etp.config
  (:require [integrant.core :as ig]))

(defn env [name default]
  (or (System/getenv name) default))

;;
;; For Integrant components
;;

(defn db
  ([] (db {}))
  ([opts]
   {:solita.etp/db (merge {:adapter "postgresql"
                           :server-name (env "DB_HOST" "localhost")
                           :port-number (env "DB_PORT" 5432)
                           :username (env "DB_USER" "etp_app")
                           :password (env "DB_PASSWORD" "etp")
                           :database-name (env "DB_DATABASE" "etp_dev")
                           :current-schema (env "DB_SCHEMA" "etp")}
                          opts)}))

(defn http-server
  ([] (http-server {}))
  ([opts]
   {:solita.etp/http-server (merge {:port (env "HTTP_SERVER_PORT" 8080)
                                    :ctx {:db (ig/ref :solita.etp/db)}}
                                   opts)}))

;;
;; Misc config
;;

(def default-trusted-iss (env "TRUSTED_JWT_ISS" "https://raw.githubusercontent.com/solita/etp-core/develop/etp-backend/src/test/resources"))
