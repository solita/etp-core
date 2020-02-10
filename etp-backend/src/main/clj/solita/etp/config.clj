(ns solita.etp.config
  (:require [integrant.core :as ig]))

(defn env [name default]
  (or (System/getenv name) default))

(defn db [server-name port-number username password database-name current-schema]
  {:solita.etp/db {:adapter "postgresql"
                   :server-name server-name
                   :port-number port-number
                   :username username
                   :password password
                   :database-name database-name
                   :current-schema current-schema}})

(defn http-server [port]
  {:solita.etp/http-server {:port port
                            :ctx {:db (ig/ref :solita.etp/db)}}})
