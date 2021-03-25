(ns solita.etp.api.viesti
  (:require [ring.util.response :as r]
            [solita.etp.api.response :as api-response]
            [solita.etp.schema.viesti :as viesti-schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.service.viesti :as viesti-service]
            [solita.etp.service.rooli :as rooli-service]
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

      :get  {:summary    "Hae kaikki käyttäjän viestiketjut."
             :parameters {:query {(schema/optional-key :limit)  (common-schema/LimitedInt 1 100)
                                  (schema/optional-key :offset) schema/Int}}
             :responses  {200 {:body [viesti-schema/Ketju]}}
             :handler    (fn [{{:keys [query]} :parameters :keys [db whoami]}]
                           (r/response (viesti-service/find-ketjut db whoami query)))}}]
    ["/count"
     {:conflicting true
      :get  {:summary   "Hae viestiketjujen lukumäärä."
             :responses {200 {:body {:count schema/Int}}}
             :handler   (fn [{:keys [db whoami]}]
                          (r/response (viesti-service/count-ketjut db whoami)))}}]
    ["/:id"
     [""
      {:conflicting true
       :get {:summary    "Hae viestiketjun tiedot"
             :parameters {:path {:id common-schema/Key}}
             :responses  {200 {:body viesti-schema/Ketju}
                          404 {:body schema/Str}}
             :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db whoami]}]
                           (api-response/get-response
                             (viesti-service/find-ketju db whoami id)
                             (str "Ketju " id " does not exists.")))}
       :put {:summary    "Päivitä viestiketjun tiedot"
             :access     (some-fn rooli-service/paakayttaja? rooli-service/laskuttaja?)
             :parameters {:path {:id common-schema/Key}
                          :body viesti-schema/KetjuUpdate}
             :responses  {200 {:body nil}
                          404 {:body schema/Str}}
             :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db parameters]}]
                           (api-response/put-response
                            (viesti-service/update-ketju! db id (:body parameters))
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
                              (str "Ketju " id " does not exists.")))}}]]]
   ["/vastaanottajaryhmat"
    {:get {:summary   "Hae kaikki vastaanottajaryhmat."
           :responses {200 {:body [common-schema/Luokittelu]}}
           :handler   (fn [{:keys [db]}]
                        (r/response (viesti-service/find-vastaanottajaryhmat db)))}}]])
