(ns solita.etp.api.kayttaja
  (:require [ring.util.response :as r]
            [schema.core :as schema]
            [solita.etp.api.response :as api-response]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.kayttaja :as kayttaja-schema]
            [solita.etp.schema.kayttaja-laatija :as kayttaja-laatija-schema]
            [solita.etp.service.kayttaja-laatija :as kayttaja-laatija-service]))

(def routes
  [["/whoami"
    {:get {:summary "Kirjautuneen käyttäjän tiedot"
           :responses {200 {:body kayttaja-schema/Whoami}}
           :handler (constantly (r/response {:id 1234 :username "Testi"}))}}]
   ["/kayttajat"
    ["/:id"
     {:get {:summary "Hae käyttäjän tiedot (laatijarekisterin tiedot mukaanlukien)"
            :parameters {:path {:id common-schema/Key}}
            :responses {200 {:body kayttaja-laatija-schema/KayttajaLaatija}
                        404 {:body schema/Str}}
            :handler (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                       (-> (kayttaja-laatija-service/find-kayttaja-laatija db id)
                           (api-response/get-response
                            (str "Käyttäjä " id " does not exist."))))}}]]])
