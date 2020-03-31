(ns solita.etp.api.kayttaja
  (:require [ring.util.response :as r]
            [schema.core :as schema]
            [solita.etp.api.response :as api-response]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.kayttaja :as kayttaja-schema]
            [solita.etp.schema.laatija :as laatija-schema]
            [solita.etp.schema.kayttaja-laatija :as kayttaja-laatija-schema]
            [solita.etp.service.kayttaja :as kayttaja-service]
            [solita.etp.service.kayttaja-laatija :as kayttaja-laatija-service]
            [solita.etp.service.laatija :as laatija-service]))

(def routes
  [["/whoami"
    {:get {:summary "Kirjautuneen käyttäjän tiedot"
           :responses {200 {:body kayttaja-schema/Kayttaja}}
           :handler (fn [{:keys [kayttaja jwt-payloads db]}]
                      (kayttaja-service/update-login!
                       db
                       (:id kayttaja)
                       (-> jwt-payloads :data :sub))
                      (r/response kayttaja))}}]
   ["/kayttajat"
    ["/:id"
     [""
      {:get {:summary "Hae käyttäjän tiedot"
             :parameters {:path {:id common-schema/Key}}
             :responses {200 {:body kayttaja-schema/Kayttaja}
                         404 {:body schema/Str}}
             :handler (fn [{{{:keys [id]} :path} :parameters :keys [db
                                                                   kayttaja]}]
                        (-> (kayttaja-service/find-kayttaja db kayttaja id)
                            (api-response/get-response
                             (str "Käyttäjä " id " does not exist."))))}
       :put {:summary "Päivitä käyttäjän ja käyttäjään liittyvän laatijan tiedot"
             :parameters {:path {:id common-schema/Key}
                          :body kayttaja-laatija-schema/KayttajaLaatijaUpdate}
             :responses {200 {:body nil}
                         404 {:body schema/Str}}
             :handler (fn [{{{:keys [id]} :path} :parameters :keys [db parameters]}]
                        (api-response/put-response
                         (kayttaja-laatija-service/update-kayttaja-laatija!
                          db
                          id
                          (:body parameters))
                         (str "Käyttäjä " id " does not exists.")))}}]
     ["/laatija"
      {:get {:summary "Hae käyttäjään liittyvät laatijatiedot"
             :parameters {:path {:id common-schema/Key}}
             :responses {200 {:body laatija-schema/Laatija}
                         404 {:body schema/Str}}
             :handler (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                        (-> (laatija-service/find-laatija-with-kayttaja-id db id)
                            (api-response/get-response
                             (str "No laatija information for käyttäjä id " id))))}}]]]
   ["/roolit"
    {:get {:summary    "Hae roolit -luokittelu"
           :responses  {200 {:body [kayttaja-schema/Rooli]}}
           :handler    (fn [_]
                         (r/response (kayttaja-service/find-roolit)))}}]])
