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
                             (yritys-service/add-yritys! db body-params)))}}]

    ["/:id"
     [""
      {:get {:summary    "Hae yrityksen perustiedot"
             :parameters {:path {:id common-schema/Key}}
             :responses  {200 {:body yritys-schema/Yritys}
                          404 {:body schema/Str}}
             :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                           (api-response/get-response
                             (yritys-service/find-yritys db id)
                             (str "Yritys " id " does not exists.")))}}]

     ["/laskutusosoitteet"
      {:get  {:summary    "Hae yrityksen kaikki laskutusosoitetiedot"
              :parameters {:path {:id common-schema/Key}}
              :responses  {200 {:body [yritys-schema/Laskutusosoite]}}
              :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                            (r/response (yritys-service/find-laskutusosoitteet db id)))}

       :post {:summary    "Lisää uusi laskutustieto yritykseen"
              :parameters {:path {:id common-schema/Key}
                           :body yritys-schema/LaskutusosoiteSave}
              :responses  {201 {:body common-schema/Id}
                           404 {:body schema/Str}}
              :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db body-params uri]}]
                            (api-response/created uri
                              (yritys-service/add-laskutusosoite! db id body-params)))}}]]]])
