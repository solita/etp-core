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
                    :responses {200 {:body [common-schema/Kayttaja]}}
                    :handler   (fn [{:keys [db]}]
                                 (r/response (valvonta-service/find-valvojat db)))}}]
    ["/:id"
     {:conflicting true
      :get         {:summary    "Hae energiatodistuksen valvonnan tila"
                    :parameters {:path {:id common-schema/Key}}
                    :responses  {200 {:body valvonta-schema/Valvonta}
                                 404 {:body schema/Str}}
                    :access     (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
                    :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db whoami]}]
                                  (api-response/get-response
                                    (valvonta-service/find-valvonta db id)
                                    (str "Energiatodistus " id " does not exists.")))}

      :put         {:summary    "Päivitä energiatodistuksen valvonnan tila"
                    :parameters {:path {:id common-schema/Key}
                                 :body valvonta-schema/Valvonta}
                    :access     rooli-service/paakayttaja?
                    :responses  {200 {:body nil}
                                 404 {:body schema/Str}}
                    :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db parameters]}]
                                  (api-response/put-response
                                    (valvonta-service/update-valvonta!
                                      db id (-> parameters :body :active))
                                    (str "Energiatodistus " id " does not exists.")))}}]]])
