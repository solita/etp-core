(ns solita.etp.api.valvonta-oikeellisuus
  (:require [solita.etp.service.rooli :as rooli-service]
            [solita.etp.service.valvonta-oikeellisuus :as valvonta-service]
            [solita.etp.schema.valvonta-oikeellisuus :as valvonta-schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.api.response :as api-response]
            [ring.util.response :as r]
            [schema.core :as schema]
            [schema-tools.core :as schema-tools]))

(def routes
  [["/valvonta/oikeellisuus"
    ["/toimenpidetyypit"
     {:conflicting true
      :get         {:summary   "Hae energiatodistusten oikeellisuuden valvonnan toimenpidetyypit."
                    :responses {200 {:body [common-schema/Luokittelu]}}
                    :access    (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
                    :handler   (fn [{:keys [db]}]
                                 (r/response (valvonta-service/find-toimenpidetyypit db)))}}]
    ["/valvojat"
     {:conflicting true
      :get         {:summary   "Hae kaikki kasittelijat."
                    :responses {200 {:body [common-schema/Kayttaja]}}
                    :handler   (fn [{:keys [db]}]
                                 (r/response (valvonta-service/find-valvojat db)))}}]

    ["/count"
     {:conflicting true
      :get         {:summary   "Hae viestiketjujen lukumäärä."
                    :responses {200 {:body {:count schema/Int}}}
                    :handler   (fn [{:keys [db whoami]}]
                                 (r/response (valvonta-service/count-valvonnat db)))}}]

    [""
     {:conflicting true
      :get         {:summary   "Hae energiatodistusten oikeellisuuden valvonnat (työjono)."
                    :responses {200 {:body [valvonta-schema/ValvontaStatus]}}
                    :access    rooli-service/paakayttaja?
                    :handler   (fn [{:keys [db whoami]}]
                                 (r/response (valvonta-service/find-valvonnat db)))}}]

    ["/:id"
     [""
      {:conflicting true
       :get         {:summary    "Hae yksittäisen valvonnan yleiset tiedot."
                     :parameters {:path {:id common-schema/Key}}
                     :responses  {200 {:body valvonta-schema/Valvonta}}
                     :access     (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
                     :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db whoami]}]
                                   (r/response (valvonta-service/find-valvonta db id)))}

       :put         {:summary    "Muuta valvonnan yleisiä tietoja."
                     :access     rooli-service/paakayttaja?
                     :parameters {:path {:id common-schema/Key}
                                  :body (schema-tools/optional-keys-schema valvonta-schema/ValvontaSave)}
                     :responses  {200 {:body nil}}
                     :handler    (fn [{{{:keys [id]} :path :keys [body]}
                                       :parameters :keys [db whoami]}]
                                   (r/response (valvonta-service/save-valvonta! db id body)))}}]

     ["/toimenpiteet"
      [""
       {:get  {:summary    "Hae energiatodistuksen valvontatoimenpiteet."
               :parameters {:path {:id common-schema/Key}}
               :responses  {200 {:body [valvonta-schema/Toimenpide]}}
               :access     (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
               :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db whoami]}]
                             (api-response/get-response
                               (valvonta-service/find-toimenpiteet db whoami id)
                               (str "Energiatodistus " id " does not exists.")))}

        :post {:summary    "Lisää energiatodistuksen valvontatoimenpide."
               :access     (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
               :parameters {:path {:id common-schema/Key}
                            :body valvonta-schema/ToimenpideAdd}
               :responses  {201 {:body common-schema/Id}
                            404 common-schema/ConstraintError}
               :handler    (fn [{{{:keys [id]} :path :keys [body]}
                                 :parameters :keys [db whoami]}]
                             (api-response/with-exceptions
                               #(api-response/created
                                  (str "/valvonta/oikeellisuus/" id "/toimenpiteet")
                                  (valvonta-service/add-toimenpide! db whoami id body))
                               [{:constraint :toimenpide-energiatodistus-id-fkey
                                 :response   404}]))}}]
      ["/:toimenpide-id"
       {:put {:summary    "Muuta toimenpiteen tietoja."
              :access     (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
              :parameters {:path {:id            common-schema/Key
                                  :toimenpide-id common-schema/Key}
                           :body valvonta-schema/ToimenpideUpdate}
              :responses  {200 {:body nil}
                           404 schema/Str}
              :handler    (fn [{{{:keys [id toimenpide-id]} :path :keys [body]}
                                :parameters :keys [db whoami]}]
                            (api-response/put-response
                              (valvonta-service/update-toimenpide!
                                db whoami id toimenpide-id body)
                              (str "Toimenpide " toimenpide-id " does not exists.")))}}]]]]])
