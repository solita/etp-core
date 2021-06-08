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
    ["/ilmoituspaikat"
     {:conflicting true
      :get         {:summary   "Hae käytönvalvonnan ilmoituspaikat"
                    :responses {200 {:body [common-schema/Luokittelu]}}
                    :access    rooli-service/paakayttaja?
                    :handler   (fn [{:keys [db]}]
                                 (r/response (valvonta-service/find-ilmoituspaikat db)))}}]
    [""
     {:conflicting true
      :get         {:summary    "Hae käytönvalvonnat (työjono)."
                    :parameters {:query valvonta-schema/ValvontaQuery}
                    :responses  {200 {:body [valvonta-kaytto-schema/ValvontaStatus]}}
                    :access     rooli-service/paakayttaja?
                    :handler    (fn [{{:keys [query]} :parameters :keys [db]}]
                                  (r/response (valvonta-service/find-valvonnat db query)))}
      :post        {:summary    "Luo uusi käytönvalvonta"
                    :access     rooli-service/paakayttaja?
                    :parameters {:body valvonta-kaytto-schema/ValvontaSave}
                    :responses  {200 {:body common-schema/Id}}
                    :handler    (fn [{{:keys [body]} :parameters :keys [db uri]}]
                                  (api-response/created
                                   uri
                                   {:id (valvonta-service/add-valvonta! db body)}))}}]
    ["/:id"
     [""
      {:conflicting true
       :get         {:summary    "Hae yksittäisen käytönvalvonnan yleiset tiedot."
                     :parameters {:path {:id common-schema/Key}}
                     :responses  {200 {:body valvonta-kaytto-schema/Valvonta}}
                     :access     rooli-service/paakayttaja?
                     :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                                   (api-response/get-response
                                     (valvonta-service/find-valvonta db id)
                                     (str "Käytönvalvonta " id " does not exist.")))}
       :put         {:summary    "Muuta käytönvalvonnan yleisiä tietoja."
                     :access     rooli-service/paakayttaja?
                     :parameters {:path {:id common-schema/Key}
                                  :body valvonta-kaytto-schema/ValvontaSave}
                     :responses  {200 {:body nil}}
                     :handler    (fn [{{{:keys [id]} :path :keys [body]}
                                      :parameters :keys [db]}]
                                   (api-response/ok|not-found
                                     (valvonta-service/update-valvonta! db id body)
                                     (str "Käytönvalvonta " id " does not exist.")))}
       :delete    {:summary    "Poista käytönvalvonta"
                   :access     rooli-service/paakayttaja?
                   :parameters {:path {:id common-schema/Key}}
                   :responses  {200 {:body nil}}
                   :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                                 (api-response/ok|not-found
                                  (valvonta-service/delete-valvonta! db id)
                                  (str "Käytönvalvonta " id " does not exist.")))}}]
     ["/henkilot"
      [""
       {:get  {:summary    "Hae käytönvalvonnan henkilöt"
               :parameters {:path {:id common-schema/Key}}
               :responses  {200 {:body [valvonta-kaytto-schema/HenkiloStatus]}}
               :access     rooli-service/paakayttaja?
               :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                             (api-response/get-response
                              (valvonta-service/find-henkilot db id)
                               (str "Käytönvalvonta " id " does not exist.")))}

        :post {:summary    "Lisää käytönvalvontaan henkilö"
               :access     rooli-service/paakayttaja?
               :parameters {:path {:id common-schema/Key}
                            :body valvonta-kaytto-schema/HenkiloSave}
               :responses  {201 {:body common-schema/Id}
                            404 common-schema/ConstraintError}
               :handler    (fn [{{{:keys [id]} :path :keys [body]} :parameters :keys [db uri]}]
                             (api-response/with-exceptions
                               #(api-response/created
                                 uri
                                 {:id (valvonta-service/add-henkilo! db id body)})
                               [{:constraint :henkilo-valvonta-id-fkey
                                 :response 404}]))}}]
      ["/:henkilo-id"
       [""
       {:get {:summary    "Hae yksittäisen henkilön tiedot."
              :parameters {:path {:id common-schema/Key
                                  :henkilo-id common-schema/Key}}
              :responses  {200 {:body valvonta-kaytto-schema/Henkilo}
                           404 {:body schema/Str}}
              :access     rooli-service/paakayttaja?
              :handler    (fn [{{{:keys [id henkilo-id]} :path} :parameters :keys [db whoami]}]
                            (api-response/get-response
                              (valvonta-service/find-henkilo db henkilo-id)
                              (str "Henkilö " id "/" henkilo-id " does not exist.")))}
        :put {:summary    "Muuta henkilön tietoja."
              :access     rooli-service/paakayttaja?
              :parameters {:path {:id common-schema/Key
                                  :henkilo-id common-schema/Key}
                           :body valvonta-kaytto-schema/HenkiloSave}
              :responses  {200 {:body nil}
                           404 {:body schema/Str}}
              :handler    (fn [{{{:keys [id henkilo-id]} :path :keys [body]}
                                :parameters :keys [db whoami]}]
                            (api-response/ok|not-found
                             (valvonta-service/update-henkilo! db henkilo-id body)
                             (str "Henkilö " id "/" henkilo-id " does not exist.")))}
        :delete {:summary    "Poista henkilö."
                 :access     rooli-service/paakayttaja?
                 :parameters {:path {:id common-schema/Key
                                     :henkilo-id common-schema/Key}}
                 :responses  {200 {:body nil}}
                 :handler    (fn [{{{:keys [id henkilo-id]} :path} :parameters :keys [db]}]
                               (api-response/ok|not-found
                                (valvonta-service/delete-henkilo! db henkilo-id)
                                (str "Henkilö " id "/" henkilo-id " does not exist.")))}}]]]]]])
