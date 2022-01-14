(ns solita.etp.api.statistics
  (:require [ring.util.response :as r]
            [solita.etp.header-middleware :as header]
            [solita.etp.schema.statistics :as statistics-schema]
            [solita.etp.service.statistics :as statistics-service]
            [solita.etp.schema.common :as common-schema]))

(defn routes [use-cache?]
  [["/statistics"
    [""
     {:get {:summary    "Hae energiatehokkuuteen liittyvää tilastotietoa"
            :middleware [(when use-cache? [header/wrap-cache-control 3600])]
            :parameters {:query statistics-schema/StatisticsQuery}
            :responses  {200 {:body statistics-schema/StatisticsResponse}}
            :handler    (fn [{{:keys [query]} :parameters :keys [db]}]
                          (r/response (statistics-service/find-statistics db query)))}}]
    ["/kayttotarkoitukset"
     {:get {:summary    "Hae tilastoinnissa käytettävät käyttötarkoitukset"
            :middleware [(when use-cache? [header/wrap-cache-control 3600])]
            :responses  {200 {:body [common-schema/Luokittelu]}}
            :handler    (fn [{:keys [db]}]
                          (r/response (statistics-service/find-kayttotarkoitukset db)))}}]]])
