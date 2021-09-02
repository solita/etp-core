(ns solita.etp.config
  (:require [clojure.string :as str]
            [integrant.core :as ig]
            [cognitect.aws.credentials :as credentials]
            [clojure.edn :as edn]))

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
                                    :thread 20
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

(def environment-alias (env "ENVIRONMENT_ALIAS" "test"))

;; Base URLs
(def service-host (env "SERVICE_HOST" "localhost:3000"))
(def index-url (str (if (str/starts-with? service-host "localhost")
                      (str "https://" service-host)
                      (str "https://private." service-host))))

;; JWT
(def trusted-jwt-iss (env "TRUSTED_JWT_ISS" "https://raw.githubusercontent.com/solita/etp-core/develop/etp-backend/src/test/resources/"))
(def data-jwt-public-key-base-url (env "DATA_JWT_PUBLIC_KEY_BASE_URL" "https://raw.githubusercontent.com/solita/etp-core/develop/etp-backend/src/test/resources/"))

;; Logout
(def keycloak-suomifi-logout-url (env "KEYCLOAK_SUOMIFI_LOGOUT_URL" index-url))
(def keycloak-virtu-logout-url (env "KEYCLOAK_VIRTU_LOGOUT_URL" index-url))
(def cognito-logout-url (env "COGNITO_LOGOUT_URL" (str index-url "?client=id=localhost")))

;; Laskutus
(def laskutus-sftp-host (env "LASKUTUS_SFTP_HOST" "localhost"))
(def laskutus-sftp-port (try
                          (Integer/parseInt (env "LASKUTUS_SFTP_PORT" "2222"))
                          (catch java.lang.NumberFormatException e
                            nil)))
(def laskutus-sftp-username (env "LASKUTUS_SFTP_USERNAME" "etp"))
(def laskutus-sftp-password (env "LASKUTUS_SFTP_PASSWORD" "etp"))
(def known-hosts-path "known_hosts")
(def laskutus-tasmaytysraportti-email-to
  (->> (str/split (env "LASKUTUS_TASMAYTYSRAPORTTI_EMAIL_TO" "etp@example.com") #",")
       (map str/trim)
       (remove str/blank?)))

;; SMTP
(def smtp-host (env "SMTP_HOST" "localhost"))
(def smtp-port (env "SMTP_PORT" "2525"))
(def smtp-username (env "SMTP_USERNAME" ""))
(def smtp-password (env "SMTP_PASSWORD" ""))

;; General email
(def email-from-email (env "EMAIL_FROM_EMAIL" "no-reply@example.com"))
(def email-from-name (env "EMAIL_FROM_NAME" "Energiatodistusrekisteri [Dev]"))
(def email-reply-to-email (env "EMAIL_REPLY_TO_EMAIL" "reply@example.com"))
(def email-reply-to-name (env "EMAIL_REPLY_TO_NAME" "Energiatodistusrekisteri [Dev]"))

;; Asha

(def asha-endpoint-url (env "ASHA_ENDPOINT_URL" nil))
(def asha-proxy? (edn/read-string (env "ASHA_PROXY" "false")))
(def asha-debug? (edn/read-string (env "ASHA_DEBUG" "false")))
