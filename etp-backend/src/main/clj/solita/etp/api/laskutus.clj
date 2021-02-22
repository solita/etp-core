(ns solita.etp.api.laskutus
  (:require [ring.util.response :as r]
            [schema.core :as schema]
            [solita.etp.security :as security]
            [solita.etp.service.laskutus :as laskutus-service]))

(def routes
  [["/laskutus"
    {:middleware [[security/wrap-db-application-name -2]]
     :post {:summary    "Käynnistä laskutusajo"
           :responses  {200 {:body nil}}
           :handler    (fn [{:keys [db aws-s3-client]}]
                         (future (laskutus-service/do-kuukauden-laskutus db aws-s3-client))
                         (r/response {}))}}]])
