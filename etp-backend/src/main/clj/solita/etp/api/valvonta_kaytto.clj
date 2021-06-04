(ns solita.etp.api.valvonta-kaytto
  (:require [ring.util.response :as r]
            [schema.core :as schema]
            [schema-tools.core :as schema-tools]
            [solita.etp.api.response :as api-response]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.service.valvonta-kaytto :as valvonta-service]
            [solita.etp.schema.valvonta :as valvonta-schema]
            [solita.etp.schema.valvonta-kaytto :as valvonta-kaytto-schema]))

(def routes
  [["/valvonta/kaytto"
    [""
     {:conflicting true
      :get         {:summary    "Hae käytönvalvonnat (työjono)."
                    :parameters {:query valvonta-schema/ValvontaQuery}
                    :responses  {200 {:body [valvonta-kaytto-schema/ValvontaStatus]}}
                    :access     rooli-service/paakayttaja?
                    :handler    (fn [{{:keys [query]} :parameters :keys [db whoami]}]
                                  (r/response (valvonta-service/find-valvonnat db query)))}}]
    ["/:id"
     [""
      {:conflicting true
       :get         {:summary    "Hae yksittäisen käytönvalvonnan yleiset tiedot."
                     :parameters {:path {:id common-schema/Key}}
                     :responses  {200 {:body valvonta-kaytto-schema/Valvonta}}
                     :access     rooli-service/paakayttaja?
                     :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db whoami]}]
                                   (api-response/get-response
                                     (valvonta-service/find-valvonta db id)
                                     (str "Käytönvalvonta " id " does not exists.")))}
       :post        {:summary    "Luo uusi käytönvalvonta"
                     :access     rooli-service/paakayttaja?
                     :parameters {:body valvonta-kaytto-schema/ValvontaSave}
                     :responses  {200 {:body common-schema/Id}}
                     :handler    (fn [{{:keys [body]} :parameters :keys [db whoami uri]}]
                                   (api-response/created
                                    uri
                                    {:id (valvonta-service/add-valvonta! db body)}))}
       :put         {:summary    "Muuta käytönvalvonnan yleisiä tietoja."
                     :access     rooli-service/paakayttaja?
                     :parameters {:path {:id common-schema/Key}
                                  :body valvonta-kaytto-schema/ValvontaSave}
                     :responses  {200 {:body nil}}
                     :handler    (fn [{{{:keys [id]} :path :keys [body]}
                                      :parameters :keys [db whoami]}]
                                   (api-response/ok|not-found
                                     (valvonta-service/update-valvonta! db id body)
                                     (str "Käytönvalvonta " id " does not exists.")))}}]]]])
