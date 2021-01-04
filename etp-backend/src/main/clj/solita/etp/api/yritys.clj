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
             :handler    (fn [{:keys [db whoami parameters uri]}]
                           (api-response/created
                            uri
                            {:id (yritys-service/add-yritys! db whoami (:body parameters))}))}

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
             :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db whoami parameters]}]
                           (api-response/put-response
                             (yritys-service/update-yritys! db whoami id (:body parameters))
                             (str "Yritys " id " does not exists.")))}}]
     ["/laatijat"
      [""
       {:get {:summary    "Hae yrityksen laatijat"
              :parameters {:path {:id common-schema/Key}}
              :responses  {200 {:body [yritys-schema/Laatija]}}
              :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                            (r/response
                              (yritys-service/find-laatijat db id)))}}]
      ["/:laatija-id"
       {:put {:summary    "Liitä laatija yritykseen - hyväksytty"
              :parameters {:path {:id         common-schema/Key
                                  :laatija-id common-schema/Key}}
              :responses  {200 {:body nil}
                           404 common-schema/ConstraintError}
              :handler    (fn [{{{:keys [id laatija-id]} :path} :parameters :keys [db whoami]}]
                            (api-response/response-with-exceptions
                              #(yritys-service/add-laatija-yritys! db whoami laatija-id id)
                              [{:constraint :laatija-yritys-laatija-id-fkey
                                :response   404}
                               {:constraint :laatija-yritys-yritys-id-fkey
                                :response   404}]))}}]]]]
   ["/laskutuskielet/"
    {:get {:summary    "Hae laskutuskielet -luokittelu"
           :responses  {200 {:body [common-schema/Luokittelu]}}
           :handler    (fn [{:keys [db]}]
                         (r/response (yritys-service/find-all-laskutuskielet db)))}}]
   ["/verkkolaskuoperaattorit/"
    {:get {:summary    "Hae verkkolaskuoperaattorit -luokittelu"
           :responses  {200 {:body [yritys-schema/Verkkolaskuoperaattori]}}
           :handler    (fn [{:keys [db]}]
                         (r/response (yritys-service/find-all-verkkolaskuoperaattorit db)))}}]])
