(ns solita.etp.api.laatija
  (:require [ring.util.response :as r]
            [solita.etp.api.response :as api-response]
            [solita.etp.schema.laatija :as laatija-schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.service.laatija :as laatija-service]
            [schema.core :as schema]))

(def routes
  [["/laatijat"
    [""
     {:post {:summary    "Lisää laatijat laatijarekisteriin"
             :parameters {:body laatija-schema/LaatijatSave}
             :responses  {201 {:body common-schema/Ids}}
             :handler    (fn [{:keys [db body-params uri]}]
                           (->> (laatija-service/add-or-update-existing-laatijat! db body-params)
                                (api-response/items-created uri)))}}]
    ["/:id"
      [""
       {:get {:summary    "Hae laatijan tiedot"
              :parameters {:path {:id common-schema/Key}}
              :responses  {200 {:body laatija-schema/Laatija}
                           404 {:body schema/Str}}
              :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                            (-> (laatija-service/find-laatija db id)
                                (api-response/get-response
                                 (str "Laatija " id " does not exist."))))}}]
      ["/yritykset"
        [""
         {:get {:summary    "Hae laatijan yritykset"
                :parameters {:path {:id common-schema/Key}}
                :responses  {200 {:body [common-schema/Key]}}
                :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                              (-> (laatija-service/find-laatija-yritykset db id)
                                  (api-response/get-response nil)))}}]
        ["/:yritys-id"
         {:put {:summary    "Liitä laatija yritykseen"
                :parameters {:path {:id common-schema/Key
                                    :yritys-id common-schema/Key}}
                :responses  {200 {:body nil}
                             404 common-schema/ConstraintError}
                :handler    (fn [{{{:keys [id yritys-id]} :path} :parameters :keys [db]}]
                              (api-response/response-with-exceptions
                                #(do
                                   (laatija-service/attach-laatija-yritys db id yritys-id)
                                   nil)
                                [{:constraint :laatija-yritys-laatija-id-fkey :response 404}
                                 {:constraint :laatija-yritys-yritys-id-fkey :response 404}]))}}]]]]
   ["/patevyydet/"
    {:get {:summary   "Hae pätevyydet-luokittelu"
           :responses {200 {:body [laatija-schema/Patevyys]}}
           :handler   (fn [_]
                        (r/response (laatija-service/find-patevyystasot)))}}]])
