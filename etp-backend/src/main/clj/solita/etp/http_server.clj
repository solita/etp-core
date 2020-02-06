(ns solita.etp.http-server
  (:require [integrant.core :as ig]
            [org.httpkit.server :as http-kit]
            [solita.etp.handler :as handler]))

(defn wrap-ctx [ctx handler]
  (fn [req]
    (handler (assoc req :ctx ctx))))

(defmethod ig/init-key :solita.etp/http-server
  [_ {:keys [ctx] :as opts}]
  (http-kit/run-server (wrap-ctx ctx #'handler/handler) (dissoc opts :ctx)))

(defmethod ig/halt-key! :solita.etp/http-server
  [_ server]
  (server :timeout 100))
