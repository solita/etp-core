(ns solita.etp.api.geo
  (:require [ring.util.response :as r]
            [solita.etp.api.response :as api-response]
            [solita.etp.schema.geo :as geo-schema]
            [solita.etp.service.geo :as geo-service]))

(def routes
  [["/maakunnat/"
    {:get {:summary    "Hae maakunnat-koodisto"
           :responses  {200 {:body [geo-schema/Maakunta]}}
           :handler    (fn [_]
                         (r/response (geo-service/find-maakunnat)))}}]])
