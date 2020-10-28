(ns solita.etp.api.energiatodistus
  (:require [ring.util.response :as r]
            [schema.core :as schema]
            [solita.common.schema :as xschema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.energiatodistus :as energiatodistus-schema]
            [solita.etp.schema.public-energiatodistus :as public-energiatodistus-schema]
            [solita.etp.schema.kayttotarkoitus :as kayttotarkoitus-schema]
            [solita.etp.schema.e-luokka :as e-luokka-schema]
            [solita.etp.api.energiatodistus-crud :as crud-api]
            [solita.etp.api.energiatodistus-xml :as xml-api]
            [solita.etp.api.energiatodistus-liite :as liite-api]
            [solita.etp.api.energiatodistus-signing :as signing-api]
            [solita.etp.api.energiatodistus-luokittelut :as luokittelut-api]
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
            [solita.etp.service.json :as json]
            [solita.etp.service.kielisyys :as kielisyys]))

(defn valid-pdf-filename? [filename id kieli]
  (= filename (format "energiatodistus-%s-%s.pdf" id kieli)))


(defn valid-pdf-filename? [filename id kieli]
  (= filename (format "energiatodistus-%s-%s.pdf" id kieli)))

(def search-exceptions [{:type :unknown-field :response 400}
                        {:type :unknown-predicate :response 400}
                        {:type :invalid-arguments :response 400}
                        {:type :schema.core/error :response 400}])

(defn pdf-route [versio]
  ["/pdf/:kieli/:filename"
   {:get {:summary    "Lataa energiatodistus PDF-tiedostona"
          :access     (some-fn rooli-service/laatija? rooli-service/paakayttaja?)
          :parameters {:path {:id common-schema/Key
                              :kieli schema/Str
                              :filename schema/Str}}
          :responses  {200 {:body nil}
                       404 {:body schema/Str}}
          :handler    (fn [{{{:keys [id kieli filename]} :path} :parameters :keys [db aws-s3-client whoami]}]
                        (if (valid-pdf-filename? filename id kieli)
                          (api-response/pdf-response
                           (energiatodistus-pdf-service/find-energiatodistus-pdf db
                                                                                 aws-s3-client
                                                                                 whoami
                                                                                 id
                                                                                 kieli)
                           filename
                           (str "Energiatodistus " id " does not exists."))
                          (r/not-found "File not found")))}}])

(def search-route
  [""
   {:get {:summary    "Hae energiatodistuksia"
          :parameters {:query energiatodistus-schema/EnergiatodistusSearch}
          :responses  {200 {:body [public-energiatodistus-schema/Energiatodistus]}}
          :access     (some-fn rooli-service/laatija? rooli-service/paakayttaja?)
          :handler    (fn [{{:keys [query]} :parameters :keys [db whoami]}]
                        (api-response/response-with-exceptions
                         #(energiatodistus-search-service/search
                           db
                           whoami
                           (update query :where json/read-value))
                         search-exceptions))}}])

(def public-routes
  (concat
   [["/energiatodistukset"
     search-route
     luokittelut-api/routes]]))

(def private-routes
  (concat
    [["/energiatodistukset"
      search-route
      ["/xlsx/energiatodistukset.xlsx"
       {:get {:summary    "Hae energiatodistusten tiedot XLSX-tiedostona"
              :parameters {:query energiatodistus-schema/EnergiatodistusSearch}
              :responses  {200 {:body nil}}
              :access     (some-fn rooli-service/laatija? rooli-service/paakayttaja?)
              :handler    (fn [{{:keys [query]} :parameters :keys [db whoami]}]
                            (api-response/with-exceptions
                              #(api-response/xlsx-response
                                 (energiatodistus-xlsx-service/find-energiatodistukset-xlsx
                                   db
                                   whoami
                                   (update query :where json/read-value))
                                 "energiatodistukset.xlsx"
                                 "Not found.")
                              search-exceptions))}}]
      ["/all"
       ["/:id"
        {:get {:summary    "Hae mikä tahansa yksittäinen energiatodistus tunnisteella (id)"
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
                           (r/response (energiatodistus-service/find-required-properties
                                         db versio)))}}]

     ["/validation/sisaiset-kuormat/:versio"
      {:get {:summary    "Hae voimassaolevan energiatodistuksen pakolliset kentät"
             :parameters {:path {:versio common-schema/Key}}
             :responses  {200 {:body [(assoc energiatodistus-schema/SisKuormat
                                        :kayttotarkoitusluokka-id common-schema/Key)]}}
             :handler    (fn [{{{:keys [versio]} :path} :parameters :keys [db]}]
                           (r/response (energiatodistus-service/find-sisaiset-kuormat
                                         db versio)))}}]]
    luokittelut-api/routes))

(def external-routes
  [["/energiatodistukset"
    ["/2013" (crud-api/post 2013
                            (-> energiatodistus-schema/EnergiatodistusSave2013
                                (dissoc :kommentti)
                                xschema/optional-key-for-maybe))]
    ["/2018" (crud-api/post 2018
                            (-> energiatodistus-schema/EnergiatodistusSave2018
                                (dissoc :kommentti)
                                xschema/optional-key-for-maybe))]
    ["/legacy"
     ["/2013" (xml-api/post 2013)]
     ["/2018" (xml-api/post 2018)]]]])
