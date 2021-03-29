(ns solita.etp.api.valvonta-oikeellisuus
  (:require [solita.etp.service.rooli :as rooli-service]
            [solita.etp.service.valvonta-oikeellisuus :as valvonta-service]
            [solita.etp.schema.valvonta-oikeellisuus :as valvonta-schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.api.response :as api-response]
            [ring.util.response :as r]))

(def routes
  [["/valvonta/oikeellisuus"
    ["/toimenpidetyypit"
     {:get {:summary   "Hae energiatodistusten oikeellisuuden valvonnan toimenpidetyypit."
            :responses {200 {:body [common-schema/Luokittelu]}}
            :access    (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
            :handler   (fn [{:keys [db]}]
                         (r/response (valvonta-service/find-toimenpidetyypit db)))}}]
    [""
     {:conflicting true
      :get         {:summary   "Hae energiatodistusten oikeellisuuden valvonnat (työjono)."
                    :responses {200 {:body [valvonta-schema/Valvonta]}}
                    :access    rooli-service/paakayttaja?
                    :handler   (fn [{:keys [db whoami]}]
                                 (r/response (valvonta-service/find-valvonnat db)))}}]

    ["/:id/toimenpiteet"
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
             :parameters {:path {:id common-schema/Key
                                 :toimenpide-id common-schema/Key}
                          :body valvonta-schema/ToimenpideUpdate}
             :responses  {201 {:body nil}
                          404 common-schema/ConstraintError}
             :handler    (fn [{{{:keys [id toimenpide-id]} :path :keys [body]}
                               :parameters :keys [db whoami]}]
                           (api-response/put-response
                             (valvonta-service/update-toimenpide!
                               db whoami id toimenpide-id body)
                             (str "Toimenpide " toimenpide-id " does not exists.")))}}]]]])
