(ns solita.etp.system
  (:require [integrant.core :as ig]
            [solita.etp.config :as config]
            [solita.etp.db]
            [solita.etp.http-server]))

(defn config []
  (merge (config/db) (config/http-server)))

(defn start! []
  (ig/init (config)))

(defn halt! [system]
  (ig/halt! system))
