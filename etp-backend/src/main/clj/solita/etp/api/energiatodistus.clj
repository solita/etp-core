(ns solita.etp.api.energiatodistus
  (:require [solita.etp.schema.energiatodistus :as energiatodistus-schema]
            [solita.etp.service.energiatodistus :as energiatodistus-service]
            [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.api.response :as api-response]))

(def routes
  [["/energiatodistukset/"
    [""
     {:post {:summary    "Lisää luonnostilaisen energiatodistuksen"
             :parameters {:body energiatodistus-schema/EnergiatodistusSave}
             :responses  {201 {:body common-schema/Id}}
             :handler    (fn [{:keys [db parameters uri]}]
                           (api-response/created uri
                                                 (energiatodistus-service/add-energiatodistus! db (:body parameters))))}
      :get  {:summary   "Hae kaikki energiatodistukset"
             :responses {200 {:body [energiatodistus-schema/Energiatodistus]}}
             :handler   (fn [{:keys [db]}]
                          (api-response/get-response
                            (energiatodistus-service/find-all-energiatodistukset db) nil))}}]
    ["/:id"
     [""
      {:get {:summary    "Hae energiatodistus"
             :parameters {:path {:id common-schema/Key}}
             :responses  {200 {:body energiatodistus-schema/Energiatodistus}
                          404 {:body schema/Str}}
             :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                           (api-response/get-response
                             (energiatodistus-service/find-energiatodistus db id)
                             (str "Energiatodistus " id " does not exists.")))}

       :put {:summary    "Päivitä energiatodistus"
             :parameters {:path {:id common-schema/Key}
                          :body energiatodistus-schema/EnergiatodistusSave}
             :responses  {200 {:body nil}
                          404 {:body schema/Str}}
             :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db parameters]}]
                           (api-response/put-response
                             (energiatodistus-service/update-energiatodistus-when-luonnos! db id (:body parameters))
                             (str "Energiatodistus " id " does not exists.")))}}]]]])