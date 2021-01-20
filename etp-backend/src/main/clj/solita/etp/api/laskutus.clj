(ns solita.etp.api.laskutus
  (:require [solita.etp.service.laskutus :as laskutus-service]
            [ring.util.response :as r]
            [schema.core :as schema]))

(def routes
  [["/laskutus"
   {:post {:summary    "Käynnistä laskutusajo"
           :responses  {200 {:body nil}}
           :handler    (fn [{:keys [db aws-s3-client]}]
                         (future (laskutus-service/do-kuukauden-laskutus db aws-s3-client))
                         (r/response {}))}}]])
