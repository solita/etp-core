(ns solita.etp.system
  (:require [integrant.core :as ig]
            [solita.etp.config :as config]
            [solita.etp.db]
            [solita.etp.http-server]
            [solita.etp.aws-s3-client]

            [clojure.tools.logging :as log]
            ))

(defn config []
  (merge (config/db) (config/http-server) (config/aws-s3-client)))

(defn start! []
  (log/info "TEMPORARY LOG FOR SFTP HOST: " config/laskutus-sftp-host)
  (ig/init (config)))

(defn halt! [system]
  (ig/halt! system))
