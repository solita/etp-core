(ns solita.etp.api.energiatodistus-signing
  (:require [solita.etp.service.rooli :as rooli-service]
            [solita.etp.service.energiatodistus :as energiatodistus-service]
            [solita.etp.api.response :as api-response]
            [solita.etp.service.energiatodistus-pdf :as energiatodistus-pdf-service]
            [solita.etp.schema.common :as common-schema]
            [schema.core :as schema]
            [solita.etp.schema.energiatodistus :as energiatodistus-schema]))

(def routes
  ["/signature"
   ["/start"
    {:post {:summary    "Siirrä energiatodistus allekirjoitus-tilaan"
            :parameters {:path {:id common-schema/Key}}
            :access rooli-service/laatija?
            :responses  {200 {:body nil}
                         404 {:body schema/Str}}
            :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db whoami]}]
                          (api-response/signature-response
                            (energiatodistus-service/start-energiatodistus-signing! db whoami id)
                            (str "Energiatodistus " id)))}}]
   ["/digest"
    {:get {:summary    "Hae PDF-tiedoston digest allekirjoitusta varten"
           :parameters {:path {:id common-schema/Key}}
           :access rooli-service/laatija?
           :responses  {200 {:body nil}
                        404 {:body schema/Str}}
           :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                         (api-response/signature-response
                           (energiatodistus-pdf-service/find-energiatodistus-digest db id)
                           (str "Energiatodistus " id)))}}]
   ["/pdf"
    {:put {:summary "Luo allekirjoitettu PDF"
           :parameters {:path {:id common-schema/Key}
                        :body energiatodistus-schema/Signature}
           :access rooli-service/laatija?
           :responses {200 {:body nil}
                       404 {:body schema/Str}}
           :handler (fn [{{{:keys [id]} :path} :parameters :keys [db parameters]}]
                      (api-response/signature-response
                        (energiatodistus-pdf-service/sign-energiatodistus-pdf
                          db
                          id
                          (:body parameters))
                        (str "Energiatodistus " id)))}}]
   ["/finish"
    {:post {:summary "Siirrä energiatodistus allekirjoitettu-tilaan"
            :parameters {:path {:id common-schema/Key}}
            :access rooli-service/laatija?
            :responses {200 {:body nil}
                        404 {:body schema/Str}}
            :handler (fn [{{{:keys [id]} :path} :parameters :keys [db whoami]}]
                       (api-response/signature-response
                         (energiatodistus-service/end-energiatodistus-signing! db whoami id)
                         (str "Energiatodistus " id)))}}]])
