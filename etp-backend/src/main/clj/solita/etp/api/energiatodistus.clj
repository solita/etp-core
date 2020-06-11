(ns solita.etp.api.energiatodistus
  (:require [ring.util.response :as r]
            [schema.core :as schema]
            [solita.common.coerce :as common-coerce]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.energiatodistus :as energiatodistus-schema]
            [solita.etp.api.energiatodistus-crud :as crud-api]
            [solita.etp.api.energiatodistus-liite :as liite-api]
            [solita.etp.api.energiatodistus-signing :as signing-api]
            [solita.etp.service.energiatodistus :as energiatodistus-service]
            [solita.etp.service.energiatodistus-pdf :as energiatodistus-pdf-service]
            [solita.etp.service.energiatodistus-xlsx :as energiatodistus-xlsx-service]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.security :as security]
            [solita.etp.api.response :as api-response]
            [schema-tools.coerce :as schema-tools-coerce]))

(defn missing-maybe-values-coercer [schema]
  (schema-tools-coerce/coercer
    (common-schema/default-value-for-maybe schema)
    schema-tools-coerce/default-key-matcher))

(def external-routes
  [["/energiatodistukset/2018" {:middleware [[security/wrap-whoami-from-basic-auth]
                                             [security/wrap-access]
                                             [security/wrap-db-application-name]]}
    ["" (crud-api/post 2018
          (common-schema/optional-key-for-maybe
            energiatodistus-schema/EnergiatodistusSave2018)
          (missing-maybe-values-coercer
            energiatodistus-schema/EnergiatodistusSave2018))]]])

(defn pdf-route [version]
  ["/pdf"
   {:get {:summary    "Lataa energiatodistus PDF-tiedostona"
          :parameters {:path {:id common-schema/Key}}
          :responses  {200 {:body nil}
                       404 {:body schema/Str}}
          :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db whoami]}]
                        (api-response/pdf-response
                          (energiatodistus-pdf-service/find-energiatodistus-pdf db whoami id)
                          (str "energiatodistus-" version "-" id ".pdf")
                          (str "Energiatodistus " id " does not exists.")))}}])

(def private-routes
  [["/energiatodistukset"
    [""
     {:get {:summary    "Hae laatijan energiatodistukset"
            :parameters {:query {(schema/optional-key :tila) schema/Int}}
            :responses  {200 {:body [energiatodistus-schema/Energiatodistus]}}
            :access     rooli-service/laatija?
            :handler    (fn [{{{:keys [tila]} :query} :parameters :keys [db whoami]}]
                          (r/response (energiatodistus-service/find-energiatodistukset-by-laatija
                                       db
                                       (:id whoami)
                                       tila)))}}]
    ["/xlsx/energiatodistukset.xlsx"
     {:get {:summary   "Lataa laatijan energiatodistuksien tiedot XLSX-tiedostona"
            :responses {200 {:body nil}
                        404 {:body schema/Str}}
            :access    rooli-service/laatija?
            :handler   (fn [{:keys [db whoami]}]
                         (api-response/xlsx-response
                           (energiatodistus-xlsx-service/find-laatija-energiatodistukset-xlsx
                             db (:id whoami))
                           "energiatodistukset.xlsx"
                           "Not found."))}}]
    ["/all"
     ["/:id"
     {:get {:summary   "Hae mikä tahansa yksittäinen energiatodistus tunnisteella (id)"
            :parameters {:path {:id common-schema/Key}}
            :responses  {200 {:body energiatodistus-schema/Energiatodistus}
                         404 {:body schema/Str}}
            :access     rooli-service/laatija?
            :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db whoami]}]
                          (api-response/get-response
                            (energiatodistus-service/find-energiatodistus db id)
                            (str "Energiatodistus " id " does not exists.")))}}]]
    ["/signed"
     {:get {:summary    "Hae allekirjoitettuja energiatodistuksia"
            :parameters {:query {:id schema/Int}}
            :responses  {200 {:body [common-schema/Key]}}
            :access     rooli-service/laatija?
            :handler    (fn [{{{:keys [id]} :query} :parameters :keys [db]}]
                          (r/response
                            (energiatodistus-service/find-signed-energiatodistukset-like-id
                              db id)))}}]
    ["/2013"
     ["" (crud-api/post 2013 energiatodistus-schema/EnergiatodistusSave2013)]
     ["/:id"
      (crud-api/gpd-routes energiatodistus-schema/Energiatodistus2013
                           energiatodistus-schema/EnergiatodistusSave2013)
      (pdf-route 2013)
      liite-api/routes
      signing-api/routes]]

    ["/2018"
     ["" (crud-api/post 2018 energiatodistus-schema/EnergiatodistusSave2018)]
     ["/:id"
      (crud-api/gpd-routes energiatodistus-schema/Energiatodistus2018
                           energiatodistus-schema/EnergiatodistusSave2018)

      (pdf-route 2018)
      liite-api/routes
      signing-api/routes]]]

   ["/kielisyys"
    {:get {:summary   "Hae energiatodistuksen kielisyysluokittelu"
           :responses {200 {:body [common-schema/Luokittelu]}}
           :handler   (fn [_] (r/response (energiatodistus-service/find-kielisyys)))}}]

   ["/laatimisvaiheet"
    {:get {:summary   "Hae energiatodistuksen laatimisvaiheluokittelu"
           :responses {200 {:body [common-schema/Luokittelu]}}
           :handler   (fn [_] (r/response (energiatodistus-service/find-laatimisvaiheet)))}}]

   ["/kayttotarkoitusluokat/:versio"
    {:get {:summary    "Hae energiatodistuksen käyttötarkoitusluokat"
           :parameters {:path {:versio common-schema/Key}}
           :responses  {200 {:body [common-schema/Luokittelu]}}
           :handler    (fn [{{{:keys [versio]} :path} :parameters :keys [db]}]
                         (r/response (energiatodistus-service/find-kayttotarkoitukset db versio)))}}]

   ["/alakayttotarkoitusluokat/:versio"
    {:get {:summary    "Hae energiatodistuksen käyttötarkoitusluokat"
           :parameters {:path {:versio common-schema/Key}}
           :responses  {200 {:body [energiatodistus-schema/Alakayttotarkoitusluokka]}}
           :handler    (fn [{{{:keys [versio]} :path} :parameters :keys [db]}]
                         (r/response (energiatodistus-service/find-alakayttotarkoitukset db versio)))}}]])
