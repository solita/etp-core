(ns solita.etp.api.laatija
  (:require [ring.util.response :as r]
            [schema.core :as schema]
            [solita.etp.api.response :as api-response]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.laatija :as laatija-schema]
            [solita.etp.schema.kayttaja-laatija :as kayttaja-laatija-schema]
            [solita.etp.service.laatija :as laatija-service]
            [solita.etp.service.kayttaja-laatija :as kayttaja-laatija-service]
            [solita.etp.service.rooli :as rooli-service]))

(def routes
  [["/laatijat"
    [""
     {:put {:summary    "Lisää laatijat laatijarekisteriin (luo myös käyttäjä)"
            :parameters {:body [kayttaja-laatija-schema/KayttajaLaatijaAdd]}
            :responses  {200 {:body [kayttaja-laatija-schema/KayttajaLaatijaAddResponse]}}
            :access     rooli-service/patevyydentoteaja?
            :handler    (fn [{:keys [db parameters uri]}]
                          (-> (kayttaja-laatija-service/upsert-kayttaja-laatijat!
                               db
                               (:body parameters))
                              (api-response/get-response
                               "Käyttäjien / laatijoiden lisääminen tai päivittäminen epäonnistui")))}}]
    ["/:id"
     ["/yritykset"
      [""
       {:get {:summary    "Hae laatijan yritykset"
              :parameters {:path {:id common-schema/Key}}
              :responses  {200 {:body [common-schema/Key]}}
              :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db whoami]}]
                            (-> (laatija-service/find-laatija-yritykset db whoami id)
                                (api-response/get-response nil)))}}]
      ["/:yritys-id"
       {:put {:summary    "Liitä laatija yritykseen"
              :parameters {:path {:id common-schema/Key
                                  :yritys-id common-schema/Key}}
              :responses  {200 {:body nil}
                           404 common-schema/ConstraintError}
              :handler    (fn [{{{:keys [id yritys-id]} :path} :parameters :keys [db whoami]}]
                            (api-response/response-with-exceptions
                             #(do
                                (laatija-service/attach-laatija-yritys db whoami id yritys-id)
                                nil)
                             {:constraint :laatija-yritys-yritys-id-fkey
                              :response 404}))}
        :delete {:summary    "Poista laatija yrityksestä"
                 :parameters {:path {:id common-schema/Key
                                     :yritys-id common-schema/Key}}
                 :responses  {200 {:body nil}}
                 :handler    (fn [{{{:keys [id yritys-id]} :path} :parameters :keys [db whoami]}]
                               (api-response/put-response
                                (laatija-service/detach-laatija-yritys db whoami id yritys-id)
                                (str "Laatija and yritys liitos " id "/" yritys-id " does not exist.")))}}]]]]
   ["/patevyydet/"
    {:get {:summary   "Hae pätevyydet-luokittelu"
           :responses {200 {:body [laatija-schema/Patevyystaso]}}
           :handler   (fn [_]
                        (r/response (laatija-service/find-patevyystasot)))}}]])
