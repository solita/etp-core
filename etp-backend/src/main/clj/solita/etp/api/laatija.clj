(ns solita.etp.api.laatija
  (:require [ring.util.response :as r]
            [solita.etp.api.response :as api-response]
            [solita.etp.schema.laatija :as laatija-schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.service.laatija :as laatija-service]
            [schema.core :as schema]))

(def laatija-routes
  [["/laatijat"
    [""
     {:post {:summary    "Lisää uuden laatijan tiedot laatijarekisteriin"
             :parameters {:body laatija-schema/LaatijaSave}
             :responses  {201 {:body common-schema/Id}}
             :handler    (fn [{:keys [db body-params uri]}]
                           (->> (laatija-service/add-laatija! db body-params)
                                (api-response/created uri)))}}]
    ["/:id"
     {:get {:summary    "Hae laatijan tiedot"
            :parameters {:path {:id schema/Num}}
            :responses  {200 {:body laatija-schema/Laatija}
                         404 {:body schema/Str}}
            :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                          (-> (laatija-service/find-laatija db id)
                              (api-response/get-response
                               (str "Laatija " id " does not exist."))))}}]]])

(def patevyys-routes
  [["/patevyydet/"
    {:get {:summary    "Hae pätevyydet-koodisto"
           :responses  {200 {:body [laatija-schema/Patevyys]}}
           :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                         (r/response (laatija-service/get-patevyydet)))}}]])
