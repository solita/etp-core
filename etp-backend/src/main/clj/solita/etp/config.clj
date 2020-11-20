(ns solita.etp.config
  (:require [integrant.core :as ig]
            [cognitect.aws.credentials :as credentials]))

; use local evn credentials in codebuild and local env
; only ecs use s3
(def use-local-env-credentials?
  (not (System/getenv "FILES_BUCKET_NAME")))

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
                                    :max-body (* 1024 1024 50)
                                    :ctx {:db (ig/ref :solita.etp/db)
                                          :aws-s3-client (ig/ref :solita.etp/aws-s3-client)}}
                                   opts)}))

(defn aws-s3-client
  ([] (aws-s3-client {}))
  ([{:keys [client bucket]}]
   {:solita.etp/aws-s3-client
    {:client (merge
              {:api :s3
               :region "eu-central-1"}
              (when use-local-env-credentials?
                {:credentials-provider (credentials/basic-credentials-provider
                                        {:access-key-id     "minio"
                                         :secret-access-key "minio123"})
                 :endpoint-override    {:protocol :http
                                        :hostname "localhost"
                                        :port     9000}})
              client)
     :bucket (or bucket (env "FILES_BUCKET_NAME" "files"))}}))

;;
;; Misc config
;;

(def local-dev-env-index "https://localhost:3000")
(def trusted-jwt-iss (env "TRUSTED_JWT_ISS" "https://raw.githubusercontent.com/solita/etp-core/develop/etp-backend/src/test/resources/"))
(def data-jwt-public-key-base-url (env "DATA_JWT_PUBLIC_KEY_BASE_URL" "https://raw.githubusercontent.com/solita/etp-core/develop/etp-backend/src/test/resources/"))
(def keycloak-suomifi-logout-url (env "KEYCLOAK_SUOMIFI_LOGOUT_URL" local-dev-env-index))
(def keycloak-virtu-logout-url (env "KEYCLOAK_VIRTU_LOGOUT_URL" local-dev-env-index))
(def cognito-logout-url (env "COGNITO_LOGOUT_URL" local-dev-env-index))
