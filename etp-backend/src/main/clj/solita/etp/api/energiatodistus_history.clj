(ns solita.etp.api.energiatodistus-history
  (:require [ring.util.response :as r]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.energiatodistus-history :as history-schema]
            [solita.etp.service.energiatodistus-history :as history-service]))

(def routes
  ["/history"
   {:get {:summary    "Hae energiatodistuksen historiatiedot."
          :access     (some-fn rooli-service/laatija? rooli-service/paakayttaja?)
          :parameters {:path {:id common-schema/Key}}
          :responses  {200 {:body history-schema/HistoryResponse}}
          :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db whoami]}]
                        (r/response (history-service/find-history db id)))}}])
