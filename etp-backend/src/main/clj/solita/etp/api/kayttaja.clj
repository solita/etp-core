(ns solita.etp.api.kayttaja
  (:require [schema.core :as schema]
            [solita.etp.schema.kayttaja :as kayttaja-schema]
            [ring.util.response :as r]))

(def routes
  [["/kayttajat/current"
    {:get {:summary "Kirjautuneen käyttäjän tiedot"
           :responses {200 {:body kayttaja-schema/Kayttaja}}
           :handler (constantly (r/response {:id 1234 :username "Testi"}))}}]])
