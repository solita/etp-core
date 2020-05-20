(ns solita.etp.api.energiatodistus
  (:require [ring.util.response :as r]
            [reitit.ring.schema :as reitit-schema]
            [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.energiatodistus :as energiatodistus-schema]
            [solita.etp.api.energiatodistus-liite :as liite-api]
            [solita.etp.api.energiatodistus-signing :as signing-api]
            [solita.etp.service.energiatodistus :as energiatodistus-service]
            [solita.etp.service.energiatodistus-pdf :as energiatodistus-pdf-service]
            [solita.etp.service.energiatodistus-xlsx :as energiatodistus-xlsx-service]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.security :as security]
            [solita.etp.api.response :as api-response]))

(def energiatodistus-2018-post
  {:summary    "Lisää luonnostilaisen energiatodistuksen"
   :parameters {:body energiatodistus-schema/EnergiatodistusSave2018}
   :responses  {201 {:body common-schema/Id}}
   :access     rooli-service/laatija?
   :handler    (fn [{:keys [db whoami parameters uri]}]
                 (api-response/created
                  uri
                  (energiatodistus-service/add-energiatodistus!
                    db whoami 2018 (:body parameters))))})

(def external-routes
  [["/energiatodistukset/2018" {:middleware [[security/wrap-whoami-from-basic-auth]
                                             [security/wrap-access]
                                             [security/wrap-db-application-name]]}
    [""
     {:post energiatodistus-2018-post}]]])

(def private-routes
  [["/energiatodistukset"
    {:get {:summary    "Hae laatijan energiatodistukset"
           :parameters {:query {(schema/optional-key :tila) schema/Int}}
           :responses  {200 {:body [energiatodistus-schema/Energiatodistus]}}
           :access     rooli-service/laatija?
           :handler    (fn [{{{:keys [tila]} :query} :parameters :keys [db whoami]}]
                         (r/response (energiatodistus-service/find-energiatodistukset-by-laatija
                                      db (:laatija whoami) tila)))}}]
   ["/energiatodistukset/2018"
    [""
     {:post energiatodistus-2018-post}]
    ["/export/energiatodistukset.xlsx"
     {:get {:summary    "Lataa laatijan energiatodistuksien tiedot XLSX-tiedostona"
            :responses  {200 {:body nil}
                         404 {:body schema/Str}}
            :access     rooli-service/laatija?
            :handler    (fn [{:keys [db whoami]}]
                          (api-response/xlsx-response
                           (energiatodistus-xlsx-service/find-laatija-energiatodistukset-xlsx
                            db
                            (:laatija whoami))
                           (str "energiatodistukset.xlsx")
                           (str "Not found.")))}}]
    ["/:id"
     [""
      {:get {:summary    "Hae energiatodistus"
             :parameters {:path {:id common-schema/Key}}
             :responses  {200 {:body energiatodistus-schema/Energiatodistus2018}
                          404 {:body schema/Str}}
             :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db whoami]}]
                           (api-response/get-response
                             (energiatodistus-service/find-energiatodistus db whoami id)
                             (str "Energiatodistus " id " does not exists.")))}

       :put {:summary    "Päivitä energiatodistus"
             :parameters {:path {:id common-schema/Key}
                          :body energiatodistus-schema/EnergiatodistusSave2018}
             :responses  {200 {:body nil}
                          404 {:body schema/Str}}
             :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db whoami parameters]}]
                           (api-response/put-response
                            (energiatodistus-service/update-energiatodistus-luonnos!
                             db
                             whoami
                             id
                             (:body parameters))
                             (str "Energiatodistus luonnos " id " does not exists.")))}
       :delete {:summary "Poista luonnostilainen energiatodistus"
                :parameters {:path {:id common-schema/Key}}
                :responses  {200 {:body nil}
                             404 {:body schema/Str}}
                :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db whoami]}]
                              (api-response/put-response
                               (energiatodistus-service/delete-energiatodistus-luonnos!
                                db
                                whoami
                                id)
                               (str "Energiatodistus luonnos " id " does not exists.")))}}]
     ["/pdf"
      {:get {:summary    "Lataa energiatodistus PDF-tiedostona"
             :parameters {:path {:id common-schema/Key}}
             :responses  {200 {:body nil}
                          404 {:body schema/Str}}
             :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db whoami]}]
                           (api-response/pdf-response
                            (energiatodistus-pdf-service/find-energiatodistus-pdf db whoami id)
                            (str "energiatodistus2018-" id ".pdf")
                            (str "Energiatodistus " id " does not exists.")))}}]
     liite-api/routes
     signing-api/routes]]
   ["/kielisyys"
    {:get {:summary   "Hae energiatodistuksen kielisyysluokittelu"
           :responses {200 {:body [common-schema/Luokittelu]}}
           :handler   (fn [_] (r/response (energiatodistus-service/find-kielisyys)))}}]

   ["/laatimisvaiheet"
    {:get {:summary   "Hae energiatodistuksen laatimisvaiheluokittelu"
           :responses {200 {:body [common-schema/Luokittelu]}}
           :handler   (fn [_] (r/response (energiatodistus-service/find-laatimisvaiheet)))}}]

   ["/kayttotarkoitusluokat/:versio"
    {:get {:summary   "Hae energiatodistuksen käyttötarkoitusluokat"
           :parameters {:path {:versio common-schema/Key}}
           :responses {200 {:body [common-schema/Luokittelu]}}
           :handler   (fn [{{{:keys [versio]} :path} :parameters :keys [db]}]
                        (r/response (energiatodistus-service/find-kayttotarkoitukset db versio)))}}]

   ["/alakayttotarkoitusluokat/:versio"
    {:get {:summary   "Hae energiatodistuksen käyttötarkoitusluokat"
           :parameters {:path {:versio common-schema/Key}}
           :responses {200 {:body [energiatodistus-schema/Alakayttotarkoitusluokka]}}
           :handler   (fn [{{{:keys [versio]} :path} :parameters :keys [db]}]
                        (r/response (energiatodistus-service/find-alakayttotarkoitukset db versio)))}}]])
