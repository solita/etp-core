(ns solita.etp.api.yritys
  (:require [ring.util.response :as r]
            [solita.etp.api.response :as api-response]
            [solita.etp.schema.yritys :as yritys-schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.service.yritys :as yritys-service]
            [schema.core :as schema]))

(def routes
  [["/yritykset"
    [""
     {:post {:summary    "Lisää uuden yrityksen tiedot yritysrekisteriin"
             :parameters {:body yritys-schema/YritysSave}
             :responses  {201 {:body common-schema/Id}}
             :handler    (fn [{:keys [db body-params uri]}]
                           (api-response/created uri
                             (yritys-service/add-yritys! db body-params)))}

      :get {:summary    "Hae kaikki yritykset"
            :responses  {200 {:body [yritys-schema/Yritys]}}
            :handler    (fn [{:keys [db]}]
                           (api-response/get-response
                              (yritys-service/find-all-yritykset db) nil))}}]

    ["/:id"
     [""
      {:get {:summary    "Hae yrityksen perustiedot"
             :parameters {:path {:id common-schema/Key}}
             :responses  {200 {:body yritys-schema/Yritys}
                          404 {:body schema/Str}}
             :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                           (api-response/get-response
                             (yritys-service/find-yritys db id)
                             (str "Yritys " id " does not exists.")))}

       :put {:summary    "Päivitä yrityksen perustiedot"
             :parameters {:path {:id common-schema/Key}
                          :body yritys-schema/YritysSave}
             :responses  {200 {:body nil}
                          404 {:body schema/Str}}
             :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db body-params]}]
                           (api-response/put-response
                             (yritys-service/update-yritys! db id body-params)
                             (str "Yritys " id " does not exists.")))}}]]]])