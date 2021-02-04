(ns solita.etp.api.viesti
  (:require [ring.util.response :as r]
            [solita.etp.api.response :as api-response]
            [solita.etp.schema.viesti :as viesti-schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.service.viesti :as viesti-service]
            [schema.core :as schema]))

(def routes
  [["/viestit"
    [""
     {:post {:summary    "Lisää uusi yleinen viestiketju."
             :parameters {:body viesti-schema/KetjuAdd}
             :responses  {201 {:body common-schema/Id}}
             :handler    (fn [{:keys [db whoami parameters uri]}]
                           (api-response/created
                             uri {:id (viesti-service/add-ketju! db whoami (:body parameters))}))}

      :get  {:summary   "Hae kaikki viestiketjut."
             :responses {200 {:body [viesti-schema/Ketju]}}
             :handler   (fn [{:keys [db]}]
                          (r/response (viesti-service/find-ketjut db)))}}]

    ["/:id"
     [""
      {:get {:summary    "Hae viestiketjun tiedot"
             :parameters {:path {:id common-schema/Key}}
             :responses  {200 {:body viesti-schema/Ketju}
                          404 {:body schema/Str}}
             :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                           (api-response/get-response
                             (viesti-service/find-ketju db id)
                             (str "Ketju " id " does not exists.")))}}]
     ["/viestit"
      {:post {:summary    "Lisää ketjuun uusi viesti"
              :parameters {:path {:id common-schema/Key}
                           :body schema/Str}
              :responses  {200 {:body nil}
                           404 {:body schema/Str}}
              :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db whoami parameters]}]
                            (api-response/put-response
                              (viesti-service/add-viesti! db whoami id (:body parameters))
                              (str "Ketju " id " does not exists.")))}}]]]])
