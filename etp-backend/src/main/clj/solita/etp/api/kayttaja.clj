(ns solita.etp.api.kayttaja
  (:require [ring.util.response :as r]
            [schema.core :as schema]
            [solita.etp.api.response :as api-response]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.kayttaja :as kayttaja-schema]
            [solita.etp.schema.laatija :as laatija-schema]
            [solita.etp.schema.rooli :as rooli-schema]
            [solita.etp.service.whoami :as whoami-service]
            [solita.etp.service.kayttaja :as kayttaja-service]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.service.laatija :as laatija-service]))

(def routes
  [["/whoami"
    {:get {:summary "Kirjautuneen käyttäjän tiedot"
           :responses {200 {:body kayttaja-schema/Whoami}}
           :handler (fn [{:keys [whoami db]}]
                      (whoami-service/update-kayttaja-with-whoami! db whoami)
                      (r/response whoami))}}]
   ["/kayttajat"
    [""
     {:post {:summary    "Lisää muu käyttäjä kuin laatija."
             :access     rooli-service/paakayttaja?
             :parameters {:body kayttaja-schema/KayttajaUpdate}
             :responses  {201 {:body common-schema/Id}}
             :handler    (fn [{:keys [db parameters uri]}]
                           (api-response/created
                             uri {:id (kayttaja-service/add-kayttaja! db (:body parameters))}))}

      :get  {:summary    "Hae kaikki muut käyttäjät paitsi laatijat"
             :access     rooli-service/paakayttaja?
             :responses  {200 {:body [kayttaja-schema/Kayttaja]}}
             :handler    (fn [{:keys [db]}]
                           (r/response (kayttaja-service/find-kayttajat db)))}}]
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
                        (api-response/ok|not-found
                         (kayttaja-service/update-kayttaja!
                          db whoami id (:body parameters))
                         (str "Käyttäjä " id " does not exists or käyttäjä is laatija.")))}}]
     ["/history"
      {:get {:summary "Hae käyttäjän muutoshistoria"
             :parameters {:path {:id common-schema/Key}}
             :responses {200 {:body [kayttaja-schema/KayttajaHistory]}}
             :handler (fn [{{{:keys [id]} :path} :parameters :keys [db whoami]}]
                        (r/response
                         (kayttaja-service/find-history db whoami id)))}}]
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
           :handler    (fn [{:keys [db]}]
                         (r/response (rooli-service/find-roolit db)))}}]])
