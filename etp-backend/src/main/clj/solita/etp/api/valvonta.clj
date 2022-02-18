(ns solita.etp.api.valvonta
  (:require [solita.etp.service.rooli :as rooli-service]
            [solita.etp.service.valvonta :as valvonta-service]
            [solita.etp.schema.valvonta :as valvonta-schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.api.response :as api-response]
            [schema.core :as schema]
            [ring.util.response :as r]))

(def routes
  [["/valvonta"
    ["/valvojat"
     {:conflicting true
      :get         {:summary   "Hae kaikki valvojat."
                    :responses {200 {:body [(assoc common-schema/Kayttaja
                                                   :passivoitu schema/Bool)]}}
                    :access    (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
                    :handler   (fn [{:keys [db]}]
                                 (r/response (valvonta-service/find-valvojat db)))}}]]])
