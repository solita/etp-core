(ns solita.etp.api.geo
  (:require [ring.util.response :as r]
            [solita.etp.api.response :as api-response]
            [solita.etp.schema.geo :as geo-schema]
            [solita.etp.service.geo :as geo-service]))

(def routes
  [["/toimintaalueet/"
    {:get {:summary    "Hae toiminta-alueet -luokittelu"
           :responses  {200 {:body [geo-schema/Toimintaalue]}}
           :handler    (fn [_]
                         (r/response (geo-service/find-toimintaalueet)))}}]])
