(ns solita.etp.api.energiatodistus
  (:require [ring.util.response :as r]
            [schema.core :as schema]
            [solita.common.schema :as xschema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.energiatodistus :as energiatodistus-schema]
            [solita.etp.schema.kayttotarkoitus :as kayttotarkoitus-schema]
            [solita.etp.schema.e-luokka :as e-luokka-schema]
            [solita.etp.api.energiatodistus-crud :as crud-api]
            [solita.etp.api.energiatodistus-liite :as liite-api]
            [solita.etp.api.energiatodistus-signing :as signing-api]
            [solita.etp.service.energiatodistus :as energiatodistus-service]
            [solita.etp.service.energiatodistus-search :as energiatodistus-search-service]
            [solita.etp.service.energiatodistus-pdf :as energiatodistus-pdf-service]
            [solita.etp.service.energiatodistus-xlsx :as energiatodistus-xlsx-service]
            [solita.etp.service.kayttotarkoitus :as kayttotarkoitus-service]
            [solita.etp.service.luokittelu :as luokittelu-service]
            [solita.etp.service.laatimisvaihe :as laatimisvaihe]
            [solita.etp.service.e-luokka :as e-luokka-service]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.security :as security]
            [solita.etp.api.response :as api-response]
            [solita.etp.service.json :as json]))

(def external-routes
  [["/energiatodistukset"
    ["/2013" (crud-api/post 2013
                            (xschema/optional-key-for-maybe
                             energiatodistus-schema/EnergiatodistusSave2013)
                            (xschema/missing-maybe-values-coercer
                             energiatodistus-schema/EnergiatodistusSave2013))]
    ["/2018" (crud-api/post 2018
                            (xschema/optional-key-for-maybe
                             energiatodistus-schema/EnergiatodistusSave2018)
                            (xschema/missing-maybe-values-coercer
                             energiatodistus-schema/EnergiatodistusSave2018))]]])

(defn valid-pdf-filename? [filename id kieli]
  (= filename (format "energiatodistus-%s-%s.pdf" id kieli)))

(defn pdf-route [versio]
  ["/pdf/:kieli/:filename"
   {:get {:summary    "Lataa energiatodistus PDF-tiedostona"
          :access     (some-fn rooli-service/laatija? rooli-service/paakayttaja?)
          :parameters {:path {:id common-schema/Key
                              :kieli schema/Str
                              :filename schema/Str}}
          :responses  {200 {:body nil}
                       404 {:body schema/Str}}
          :handler    (fn [{{{:keys [id kieli filename]} :path} :parameters :keys [db whoami]}]
                        (if (valid-pdf-filename? filename id kieli)
                          (api-response/pdf-response
                           (energiatodistus-pdf-service/find-energiatodistus-pdf db
                                                                                 whoami
                                                                                 id
                                                                                 kieli)
                           filename
                           (str "Energiatodistus " id " does not exists."))
                          (r/not-found "File not found")))}}])

(def private-routes
  [["/energiatodistukset"
    [""
     {:get {:summary    "Hae energiatodistuksia"
            :parameters {:query {(schema/optional-key :tila)   schema/Int ;; deprecated
                                 (schema/optional-key :sort)   schema/Str
                                 (schema/optional-key :order)  schema/Str
                                 (schema/optional-key :limit)  schema/Int
                                 (schema/optional-key :offset) schema/Int
                                 (schema/optional-key :where)  schema/Str}}
            :responses  {200 {:body [energiatodistus-schema/Energiatodistus]}}
            :access     (some-fn rooli-service/laatija? rooli-service/paakayttaja?)
            :handler    (fn [{{{:keys [where sort order limit offset]} :query} :parameters
                                :keys [db whoami]}]
                          (api-response/response-with-exceptions
                            #(energiatodistus-search-service/search
                              db
                              {:where (json/read-value where)
                               :sort  sort :order order
                               :limit limit :offset offset})
                            [{:type :unknown-field :response 400}
                             {:type :unknown-predicate :response 400}
                             {:type :invalid-arguments :response 400}
                             {:type :schema.core/error :response 400}]))}}]
    ["/xlsx/energiatodistukset.xlsx"
     {:get {:summary   "Lataa laatijan energiatodistuksien tiedot XLSX-tiedostona"
            :responses {200 {:body nil}
                        404 {:body schema/Str}}
            :access    (some-fn rooli-service/laatija? rooli-service/paakayttaja?)
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
            :access     (some-fn rooli-service/laatija? rooli-service/paakayttaja?)
            :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db whoami]}]
                          (api-response/get-response
                            (energiatodistus-service/find-energiatodistus db id)
                            (str "Energiatodistus " id " does not exists.")))}}]]
    ["/replaceable"
     {:get {:summary    "Hae korvattavia energiatodistuksia"
            :parameters {:query {:id schema/Int}}
            :responses  {200 {:body [common-schema/Key]}}
            :access     (some-fn rooli-service/laatija? rooli-service/paakayttaja?)
            :handler    (fn [{{{:keys [id]} :query} :parameters :keys [db]}]
                          (r/response
                            (energiatodistus-service/find-replaceable-energiatodistukset-like-id
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
           :handler   (fn [_] (r/response (laatimisvaihe/find-laatimisvaiheet)))}}]

   ["/kayttotarkoitusluokat/:versio"
    {:get {:summary    "Hae energiatodistuksen käyttötarkoitusluokat"
           :parameters {:path {:versio common-schema/Key}}
           :responses  {200 {:body [common-schema/Luokittelu]}}
           :handler    (fn [{{{:keys [versio]} :path} :parameters :keys [db]}]
                         (r/response (kayttotarkoitus-service/find-kayttotarkoitukset db versio)))}}]

   ["/alakayttotarkoitusluokat/:versio"
    {:get {:summary    "Hae energiatodistuksen käyttötarkoitusluokat"
           :parameters {:path {:versio common-schema/Key}}
           :responses  {200 {:body [kayttotarkoitus-schema/Alakayttotarkoitusluokka]}}
           :handler    (fn [{{{:keys [versio]} :path} :parameters :keys [db]}]
                         (r/response (kayttotarkoitus-service/find-alakayttotarkoitukset db versio)))}}]

   ["/lammitysmuoto"
    {:get {:summary    "Hae energiatodistuksen lämmitysmuodot"
           :responses  {200 {:body [common-schema/Luokittelu]}}
           :handler    (fn [{:keys [db]}]
                         (r/response (luokittelu-service/find-lammitysmuodot db)))}}]

   ["/lammonjako"
    {:get {:summary    "Hae energiatodistuksen lämmönjaot"
           :responses  {200 {:body [common-schema/Luokittelu]}}
           :handler    (fn [{:keys [db]}]
                         (r/response (luokittelu-service/find-lammonjaot db)))}}]

   ["/ilmanvaihtotyyppi"
    {:get {:summary    "Hae energiatodistuksen ilmanvaihtotyypit"
           :responses  {200 {:body [common-schema/Luokittelu]}}
           :handler    (fn [{:keys [db]}]
                         (r/response (luokittelu-service/find-ilmanvaihtotyypit db)))}}]

   ["/e-luokka/:versio/:alakayttotarkoitusluokka/:nettoala/:e-luku"
    {:get {:summary    "Laske energiatodistukselle energiatehokkuusluokka"
           :parameters {:path {:versio common-schema/Key
                               :alakayttotarkoitusluokka schema/Str
                               :nettoala common-schema/FloatPos
                               :e-luku common-schema/FloatPos}}
           :responses  {200 {:body e-luokka-schema/ELuokka}}
           :handler    (fn [{{{:keys [versio alakayttotarkoitusluokka nettoala e-luku]} :path}
                            :parameters :keys [db]}]
                         (api-response/get-response
                          (e-luokka-service/find-e-luokka-info db
                                                               versio
                                                               alakayttotarkoitusluokka
                                                               nettoala
                                                               e-luku)
                          "Could not find luokittelu with given versio and alakayttotarkoitusluokka"))}}]

   ["/validation/numeric/:versio"
    {:get {:summary    "Hae energiatodistuksen numeroarvojen validointisäännöt"
           :parameters {:path {:versio common-schema/Key}}
           :responses  {200 {:body [energiatodistus-schema/NumericValidation]}}
           :handler    (fn [{{{:keys [versio]} :path} :parameters :keys [db]}]
                         (r/response (energiatodistus-service/find-numeric-validations
                                       db versio)))}}]

   ["/validation/required/:versio"
    {:get {:summary    "Hae voimassaolevan energiatodistuksen pakolliset kentät"
           :parameters {:path {:versio common-schema/Key}}
           :responses  {200 {:body [schema/Str]}}
           :handler    (fn [{{{:keys [versio]} :path} :parameters :keys [db]}]
                         (api-response/get-response
                           (energiatodistus-service/find-required-properties db versio)
                           (str "Versio " versio " does not exists.")))}}]])
