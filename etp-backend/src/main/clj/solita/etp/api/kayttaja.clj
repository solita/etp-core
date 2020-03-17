(ns solita.etp.api.kayttaja
  (:require [ring.util.response :as r]
            [schema.core :as schema]
            [solita.etp.api.response :as api-response]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.kayttaja :as kayttaja-schema]
            [solita.etp.schema.laatija :as laatija-schema]
            [solita.etp.service.laatija :as laatija-service]))

(def routes
  [["/whoami"
    {:get {:summary "Kirjautuneen käyttäjän tiedot"
           :responses {200 {:body kayttaja-schema/Kayttaja}}
           :handler (constantly (r/response {:id 1234 :username "Testi"}))}}]
   ["/kayttajat"
    ["/:id"
     {:get {:summary "Hae käyttäjän tiedot (laatijarekisterin tiedot mukaanlukien)"
            :parameters {:path {:id common-schema/Key}}
            :responses {200 {:body laatija-schema/Laatija}
                        404 {:body schema/Str}}
            :handler (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                       (-> (laatija-service/find-laatija db id)
                           (api-response/get-response
                            (str "Käyttäjä " id " does not exist."))))}}]]])
