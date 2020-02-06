(ns solita.etp.api.yritys
  (:require [ring.util.response :as r]
            [solita.etp.api.response :as api-response]
            [solita.etp.schema.yritys :as yritys-schema]
            [solita.etp.service.yritys :as yritys-service]
            [schema.core :as schema]))

(def routes
  [["/yritykset"
    [""
     {:post {:summary    "Tallenna yrityksen tiedot"
            :parameters {:body yritys-schema/YritysSave}
            :responses  {200 {:body schema/Str}}
            :handler    (fn [{:keys [db body-params]}]
                          (r/response (str (yritys-service/add-yritys! db body-params))))}}]

    ["/:id"
      {:get {:summary    "Hae yrityksen tiedot"
             :parameters {:path {:id schema/Num}}
             :responses  {200 {:body yritys-schema/Yritys}
                          404 {:body schema/Str}}
             :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                           (api-response/get-response
                             (yritys-service/find-yritys db id)
                             (str "Yritys " id " does not exists.")))}}]]])