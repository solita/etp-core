(ns solita.etp.http-server
  (:require [integrant.core :as ig]
            [org.httpkit.server :as http-kit]
            [solita.etp.handler :as handler]))

(defmethod ig/init-key :solita.etp/http-server
  [_ {:keys [ctx] :as opts}]
  (assoc opts :server (http-kit/run-server (handler/handler ctx)
                                           (dissoc opts :ctx))))

(defmethod ig/halt-key! :solita.etp/http-server
  [_ {:keys [server]}]
  (server :timeout 100))
