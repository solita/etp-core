(ns solita.etp.http-server
  (:require [integrant.core :as ig]
            [org.httpkit.server :as http-kit]))

(defn hello-handler [req]
  {:status 200
   :body {:msg "Hello from HTTP server!"
          :req req}})

(defmethod ig/init-key :solita.etp/http-server
  [_ opts]
  (assoc opts :server (http-kit/run-server hello-handler opts)))

(defmethod ig/halt-key! :solita.etp/http-server
  [_ {:keys [server]}]
  (server :timeout 100))
