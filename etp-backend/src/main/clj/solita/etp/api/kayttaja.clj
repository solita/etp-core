(ns solita.etp.api.kayttaja
  (:require [ring.util.response :as r]
            [schema.core :as schema]
            [solita.etp.api.response :as api-response]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.kayttaja :as kayttaja-schema]
            [solita.etp.schema.laatija :as laatija-schema]
            [solita.etp.service.kayttaja :as kayttaja-service]
            [solita.etp.service.laatija :as laatija-service]))

(def routes
  [["/whoami"
    {:get {:summary "Kirjautuneen käyttäjän tiedot"
           :responses {200 {:body kayttaja-schema/Whoami}}
           :handler (constantly (r/response {:id 1234 :username "Testi"}))}}]
   ["/kayttajat"
    ["/:id"
     [""
      {:get {:summary "Hae käyttäjän tiedot"
             :parameters {:path {:id common-schema/Key}}
             :responses {200 {:body kayttaja-schema/Kayttaja}
                         404 {:body schema/Str}}
             :handler (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                        (-> (kayttaja-service/find-kayttaja db id)
                            (api-response/get-response
                             (str "Käyttäjä " id " does not exist."))))}}]
     ["/laatija"
      {:get {:summary "Hae käyttäjään liittyvät laatijatiedot"
             :parameters {:path {:id common-schema/Key}}
             :responses {200 {:body laatija-schema/Laatija}
                         404 {:body schema/Str}}
             :handler (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                        (-> (laatija-service/find-laatija-with-kayttaja-id db id)
                            (api-response/get-response
                             (str "No laatija information for käyttäjä id " id))))}}]]]])
