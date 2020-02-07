(ns solita.etp.system
  (:require [integrant.core :as ig]
            [solita.etp.db]
            [solita.etp.http-server]))

;; TODO read env variables and override keys in this configuration
(def config {:solita.etp/db {:adapter "postgresql"
                             :server-name "localhost"
                             :username "etp_app"
                             :password "etp"
                             :database-name "postgres"}
             :solita.etp/http-server {:port 8080
                                      :ctx {:db (ig/ref :solita.etp/db)}}})

(defn start! []
  (ig/init config))
