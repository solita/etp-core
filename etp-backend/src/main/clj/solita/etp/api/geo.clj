(ns solita.etp.api.geo
  (:require [ring.util.response :as r]
            [solita.etp.schema.geo :as geo-schema]
            [solita.etp.service.geo :as geo-service]
            [solita.etp.schema.common :as common-schema]))

(def routes
  [["/toimintaalueet/"
    {:get {:summary    "Hae kaikki toiminta-alueet"
           :responses  {200 {:body [geo-schema/Toimintaalue]}}
           :handler    (fn [{:keys [db]}]
                         (r/response (geo-service/find-all-toiminta-alueet db)))}}]

   ["/countries/"
    {:get {:summary    "Hae kaikki maat"
           :responses  {200 {:body [geo-schema/Country]}}
           :handler    (fn [{:keys [db]}]
                         (r/response (geo-service/find-all-countries db)))}}]

   ["/postinumerot/"
    {:get {:summary   "Hae kaikki postinumerot"
           :responses {200 {:body [common-schema/Luokittelu]}}
           :handler   (fn [{:keys [db]}]
                        (r/response (geo-service/find-all-postinumerot db)))}}]])
