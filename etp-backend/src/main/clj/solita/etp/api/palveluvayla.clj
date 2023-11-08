(ns solita.etp.api.palveluvayla
  (:require [schema.core :as s]
            [solita.etp.api.response :as api-response]
            [solita.etp.schema.common :as schema.common]
            [solita.etp.schema.energiatodistus :as schema.energiatodistus]
            [solita.etp.service.energiatodistus :as service.energiatodistus]
            [solita.etp.service.energiatodistus-pdf :as service.energiatodistus-pdf]
            [solita.etp.service.energiatodistus-search :as service.energiatodistus-search]))

(def accept-language-header {(s/optional-key :accept-language) schema.common/AcceptLanguage})

(def i-am-paakayttaja {:rooli 2})
(def version-equals-2013 ["=" "energiatodistus.versio" 2013])
(def version-equals-2018 ["=" "energiatodistus.versio" 2018])

(def routes ["/energiatodistukset"
             ["/pdf/:id"
              ["" {:get {:summary    "Hae PDF-muotoinen energiatodistus tunnuksen id:llä"
                         :parameters {:path   {:id schema.common/Key}
                                      :header accept-language-header}
                         :responses  {200 {:body nil}
                                      404 {:body s/Str}}
                         :handler    (fn [{:keys                               [db aws-s3-client],
                                           {{:keys [accept-language]} :header} :parameters
                                           {{:keys [id]} :path}                :parameters}]
                                       (let [language-preference-order (if accept-language
                                                                         (->> accept-language
                                                                              (sort-by second)
                                                                              (reverse)
                                                                              (map first))
                                                                         ["fi" "sv"])]
                                         (api-response/pdf-response ; Return the first language version that exists if any
                                           (some identity (->> language-preference-order
                                                               (map #(service.energiatodistus-pdf/find-energiatodistus-pdf db
                                                                                                                           aws-s3-client
                                                                                                                           i-am-paakayttaja
                                                                                                                           id
                                                                                                                           %))))
                                           "energiatodistus.pdf"
                                           (str "Energiatodistus " id " does not exists."))))
                         :openapi    {:responses {200 {:description "PDF-muotoinen energiatodistus"
                                                       :content     {:application/pdf {:schema {:type   "string"
                                                                                                :format "binary"}}}}}}}}]]
             ["/json"
              ["/any"
               ["" {:get {:summary    "Hae energiatodistuksia json-muodossa. Palauttaa jaetut kentät sekä 2013, että 2018 lain mukaisista energiatodistuksista"
                          :parameters {:query {:rakennustunnus schema.common/Rakennustunnus}}
                          :responses  {200 {:body [schema.energiatodistus/EnergiatodistusForAnyLaatija]}}
                          :handler    (fn [{{:keys [query]} :parameters :keys [db]}]
                                        (api-response/get-response
                                          (service.energiatodistus-search/search
                                            db
                                            i-am-paakayttaja
                                            {:where [[["=" "energiatodistus.perustiedot.rakennustunnus" (:rakennustunnus query)]]]}
                                            schema.energiatodistus/EnergiatodistusForAnyLaatija)
                                          (str "Virhe haussa")))}}]
               ["/:id" {:get {:summary    "Hae yksittäinen energiatodistus todistuksen tunnuksen perusteella. Vastaus sisältää vain kentät jotka ovat yhteisiä 2013 ja 2018 versioille."
                              :parameters {:path {:id schema.common/Key}}
                              :responses  {200 {:body schema.energiatodistus/EnergiatodistusForAnyLaatija}
                                           404 {:body s/Str}}
                              :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                                            (api-response/get-response
                                              (service.energiatodistus/find-energiatodistus-any-laatija db id)
                                              (str "Energiatodistus " id " does not exists.")))}}]]
              ["/2013"
               ["" {:get {:summary    "Hae energiatodistuksia, jotka on laadittu vuoden 2013 säännösten mukaan"
                          :parameters {:query {:rakennustunnus schema.common/Rakennustunnus}}
                          :responses  {200 {:body [schema.energiatodistus/Energiatodistus2013]}}
                          :handler    (fn [{{:keys [query]} :parameters :keys [db]}]
                                        (api-response/get-response
                                          (service.energiatodistus-search/search
                                            db
                                            i-am-paakayttaja
                                            {:where [[["=" "energiatodistus.perustiedot.rakennustunnus" (:rakennustunnus query)]
                                                      version-equals-2013]]}
                                            schema.energiatodistus/Energiatodistus2013)
                                          (str "Virhe haussa")))}}]
               ["/:id" {:get {:summary    "Hae yksittäinen vuoden 2013 säännösten mukainen energiatodistus todistuksen tunnuksen perusteella"
                              :parameters {:path {:id schema.common/Key}}
                              :responses  {200 {:body schema.energiatodistus/Energiatodistus2013}
                                           404 {:body s/Str}}
                              :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                                            (api-response/get-response
                                              (-> (service.energiatodistus/find-energiatodistus db id)
                                                  (#(if (= (:versio %) 2013) % nil)))
                                              (str "Energiatodistus " id " does not exists.")))}}]]
              ["/2018"
               ["" {:get {:summary    "Hae energiatodistuksia, jotka on laadittu vuoden 2018 säännösten mukaan"
                          :parameters {:query {:rakennustunnus schema.common/Rakennustunnus}}
                          :responses  {200 {:body [schema.energiatodistus/Energiatodistus2018]}}
                          :handler    (fn [{{:keys [query]} :parameters :keys [db]}]
                                        (api-response/get-response
                                          (service.energiatodistus-search/search
                                            db
                                            i-am-paakayttaja
                                            {:where [[["=" "energiatodistus.perustiedot.rakennustunnus" (:rakennustunnus query)]
                                                      version-equals-2018]]}
                                            schema.energiatodistus/Energiatodistus2018)
                                          (str "Virhe haussa")))}}]
               ["/:id" {:get {:summary    "Hae yksittäinen vuoden 2018 säännösten mukainen energiatodistus todistuksen tunnuksen perusteella"
                              :parameters {:path {:id schema.common/Key}}
                              :responses  {200 {:body schema.energiatodistus/Energiatodistus2018}
                                           404 {:body s/Str}}
                              :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                                            (api-response/get-response
                                              (-> (service.energiatodistus/find-energiatodistus db id)
                                                  (#(if (= (:versio %) 2018) % nil)))
                                              (str "Energiatodistus " id " does not exists.")))}}]]]])
