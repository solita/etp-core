(ns solita.etp.api.kayttaja
  (:require [ring.util.response :as r]
            [schema.core :as schema]
            [solita.etp.api.response :as api-response]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.kayttaja :as kayttaja-schema]
            [solita.etp.schema.laatija :as laatija-schema]
            [solita.etp.schema.rooli :as rooli-schema]
            [solita.etp.service.kayttaja :as kayttaja-service]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.service.laatija :as laatija-service]))

(def routes
  [["/whoami"
    {:get {:summary "Kirjautuneen käyttäjän tiedot"
           :responses {200 {:body kayttaja-schema/Whoami}}
           :handler (fn [{:keys [whoami jwt-payloads db]}]
                      (kayttaja-service/update-kayttaja-with-whoami! db whoami)
                      (r/response whoami))}}]
   ["/kayttajat"
    ["/:id"
     [""
      {:get {:summary "Hae minkä tahansa käyttäjän käyttäjätiedot"
             :parameters {:path {:id common-schema/Key}}
             :responses {200 {:body kayttaja-schema/Kayttaja}
                         404 {:body schema/Str}}
             :handler (fn [{{{:keys [id]} :path} :parameters :keys [db whoami]}]
                        (-> (kayttaja-service/find-kayttaja db whoami id)
                            (api-response/get-response
                             (str "Käyttäjä " id " does not exist."))))}

       :put {:summary (str "Päivitä käyttäjän (paitsi laatija) tiedot."
                           "Laatijan tietojen päivittämiseen on eri palvelu.")
             :parameters {:path {:id common-schema/Key}
                          :body kayttaja-schema/KayttajaUpdate}
             :responses {200 {:body nil}
                         404 {:body schema/Str}}
             :handler (fn [{{{:keys [id]} :path} :parameters
                            :keys [db whoami parameters]}]
                        (api-response/put-response
                         (kayttaja-service/update-kayttaja!
                          db whoami id (:body parameters))
                         (str "Käyttäjä " id " does not exists or käyttäjä is laatija.")))}}]
     ["/laatija"
      {:get {:summary "Hae käyttäjään liittyvät laatijatiedot."
             :parameters {:path {:id common-schema/Key}}
             :responses {200 {:body laatija-schema/Laatija}
                         404 {:body schema/Str}}
             :handler (fn [{{{:keys [id]} :path} :parameters :keys [db whoami]}]
                        (-> (laatija-service/find-laatija-by-id
                             db whoami id)
                            (api-response/get-response
                             (str "No laatija information for käyttäjä id " id))))}}]]]
   ["/roolit"
    {:get {:summary    "Hae roolit -luokittelu"
           :responses  {200 {:body [rooli-schema/Rooli]}}
           :handler    (fn [_]
                         (r/response (rooli-service/find-roolit)))}}]])
