(ns solita.etp.api.palveluvayla
  (:require [schema.core :as s]
            [solita.etp.schema.common :as schema.common]
            [solita.etp.schema.energiatodistus :as schema.energiatodistus]))

(def accept-language-header {(s/optional-key :accept-language) schema.common/AcceptLanguage})

(def routes ["/energiatodistukset"
             ["/pdf/:id"
              ["" {:get {:summary    "Hae PDF-muotoinen energiatodistus tunnuksen id:llä"
                         :parameters {:path   {:id schema.common/Key}
                                      :header accept-language-header}
                         :responses  {200 {:body nil}
                                      404 {:body s/Str}}
                         :handler    (constantly {:status 200})
                         :openapi    {:responses {200 {:description "PDF-muotoinen energiatodistus"
                                                       :content     {:application/pdf {:schema {:type   "string"
                                                                                                :format "binary"}}}}}}}}]]
             ["/json"
              ["/any"
               ["" {:get {:summary    "Hae energiatodistuksia json-muodossa. Palauttaa jaetut kentät sekä 2013, että 2018 lain mukaisista energiatodistuksista"
                          :parameters {:query {:rakennustunnus schema.common/Rakennustunnus}}
                          :responses  {200 {:body [schema.energiatodistus/EnergiatodistusForAnyLaatija]}}
                          :handler    (constantly {:status 200})}}]
               ["/:id" {:get {:summary    "Hae yksittäinen energiatodistus todistuksen tunnuksen perusteella. Vastaus sisältää vain kentät jotka ovat yhteisiä 2013 ja 2018 versioille."
                              :parameters {:path {:id schema.common/Key}}
                              :responses  {200 {:body [schema.energiatodistus/EnergiatodistusForAnyLaatija]}
                                           404 {:body s/Str}}
                              :handler    (constantly {:status 200})}}]]
              ["/2013"
               ["" {:get {:summary   "Hae energiatodistuksia, jotka on laadittu vuoden 2013 säännösten mukaan"
                          :responses {200 {:body [schema.energiatodistus/Energiatodistus2013]}}
                          :handler   (constantly {:status 200})}}]
               ["/:id" {:get {:summary    "Hae yksittäinen vuoden 2013 säännösten mukainen energiatodistus todistuksen tunnuksen perusteella"
                              :parameters {:path {:id schema.common/Key}}
                              :responses  {200 {:body schema.energiatodistus/Energiatodistus2013}
                                           404 {:body s/Str}}
                              :handler    (constantly {:status 200})}}]]
              ["/2018"
               ["" {:get {:summary   "Hae energiatodistuksia, jotka on laadittu vuoden 2018 säännösten mukaan"
                          :responses {200 {:body [schema.energiatodistus/Energiatodistus2018]}}
                          :handler   (constantly {:status 200})}}]
               ["/:id" {:get {:summary    "Hae yksittäinen vuoden 2018 säännösten mukainen energiatodistus todistuksen tunnuksen perusteella"
                              :parameters {:path {:id schema.common/Key}}
                              :responses  {200 {:body schema.energiatodistus/Energiatodistus2018}
                                           404 {:body s/Str}}
                              :handler    (constantly {:status 200})}}]]]])
