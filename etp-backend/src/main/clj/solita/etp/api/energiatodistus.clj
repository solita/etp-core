(ns solita.etp.api.energiatodistus
  (:require [ring.util.response :as r]
            [reitit.ring.schema :as reitit-schema]
            [solita.etp.schema.energiatodistus :as energiatodistus-schema]
            [solita.etp.service.energiatodistus :as energiatodistus-service]
            [solita.etp.schema.liite :as liite-schema]
            [solita.etp.service.liite :as liite-service]
            [solita.etp.service.energiatodistus-pdf :as energiatodistus-pdf-service]
            [solita.etp.service.energiatodistus-xlsx :as energiatodistus-xlsx-service]
            [schema.core :as schema]
            [solita.etp.security :as security]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.api.response :as api-response]))

(def energiatodistus-2018-post
  {:summary    "Lisää luonnostilaisen energiatodistuksen"
   :parameters {:body energiatodistus-schema/EnergiatodistusSave2018}
   :responses  {201 {:body common-schema/Id}}
   :handler    (fn [{:keys [db whoami parameters uri]}]
                 (api-response/created
                  uri
                  (energiatodistus-service/add-energiatodistus!
                    db whoami 2018 (:body parameters))))})

(def external-routes
  [["/energiatodistukset/2018" {:middleware [[security/wrap-whoami-from-basic-auth]
                                             [security/wrap-access]]}
    [""
     {:post energiatodistus-2018-post}]]])

(def private-routes
  [["/energiatodistukset"
    {:get {:summary    "Hae laatijan energiatodistukset"
           :responses  {200 {:body [energiatodistus-schema/Energiatodistus]}}
           :handler    (fn [{:keys [db whoami]}]
                         (r/response (energiatodistus-service/find-energiatodistukset-by-laatija
                                      db (:laatija whoami))))}}]
   ["/energiatodistukset/2018"
    [""
     {:post energiatodistus-2018-post}]
    ["/export/energiatodistukset.xlsx"
     {:get {:summary    "Lataa laatijan energiatodistuksien tiedot XLSX-tiedostona"
            :responses  {200 {:body nil}
                         404 {:body schema/Str}}
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
             :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                           (api-response/get-response
                             (energiatodistus-service/find-energiatodistus db id)
                             (str "Energiatodistus " id " does not exists.")))}

       :put {:summary    "Päivitä energiatodistus"
             :parameters {:path {:id common-schema/Key}
                          :body energiatodistus-schema/EnergiatodistusSave2018}
             :responses  {200 {:body nil}
                          404 {:body schema/Str}}
             :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db parameters]}]
                           (api-response/put-response
                             (energiatodistus-service/update-energiatodistus-luonnos! db id (:body parameters))
                             (str "Energiatodistus luonnos " id " does not exists.")))}
       :delete {:summary "Poista luonnostilainen energiatodistus"
                :parameters {:path {:id common-schema/Key}}
                :responses  {200 {:body nil}
                             404 {:body schema/Str}}
                :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                              (api-response/put-response
                                (energiatodistus-service/delete-energiatodistus-luonnos! db id)
                                (str "Energiatodistus luonnos " id " does not exists.")))}}]
     ["/pdf"
      {:get {:summary    "Lataa energiatodistus PDF-tiedostona"
             :parameters {:path {:id common-schema/Key}}
             :responses  {200 {:body nil}
                          404 {:body schema/Str}}
             :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                           (api-response/pdf-response
                            (energiatodistus-pdf-service/find-energiatodistus-pdf db id)
                            (str "energiatodistus2018-" id ".pdf")
                            (str "Energiatodistus " id " does not exists.")))}}]
     ["/liitteet/files"
      {:post {:summary "Energiatodistuksen liitteiden lisäys tiedostoista."
              :parameters {:path {:id common-schema/Key}
                           :multipart {:files (schema/conditional
                                                vector? [reitit-schema/TempFilePart]
                                                :else reitit-schema/TempFilePart)}}
              :responses {201 {:body nil}
                          404 common-schema/ConstraintError}
              :handler (fn [{{{:keys [id]} :path {:keys [files]} :multipart} :parameters
                             :keys [db whoami]}]
                         (api-response/response-with-exceptions 201
                            #(liite-service/add-liitteet-from-files
                               db whoami id (if (vector? files) files [files]))
                            [{:constraint :liite-energiatodistus-id-fkey :response 404}]))}}]
     ["/liitteet"
      {:get {:summary "Hae energiatodistuksen liitteet."
             :parameters {:path {:id common-schema/Key}}
             :responses {200 {:body [liite-schema/Liite]}}
             :handler (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                        (r/response (liite-service/find-energiatodistus-liitteet db id)))}}]
     ["/signature"
      ["/start"
       {:post {:summary    "Siirrä energiatodistus allekirjoitus-tilaan"
               :parameters {:path {:id common-schema/Key}}
               :responses  {200 {:body nil}
                            404 {:body schema/Str}}
               :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                             (api-response/signature-response
                              (energiatodistus-service/start-energiatodistus-signing! db id)
                              (str "Energiatodistus " id)))}}]
      ["/digest"
       {:get {:summary    "Hae PDF-tiedoston digest allekirjoitusta varten"
              :parameters {:path {:id common-schema/Key}}
              :responses  {200 {:body nil}
                           404 {:body schema/Str}}
              :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                            (api-response/signature-response
                             (energiatodistus-pdf-service/find-energiatodistus-digest db id)
                             (str "Energiatodistus " id)))}}]
      ["/pdf"
       {:put {:summary    "Luo allekirjoitettu PDF"
              :parameters {:path {:id common-schema/Key}
                           :body energiatodistus-schema/Signature}
              :responses  {200 {:body nil}
                           404 {:body schema/Str}}
              :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db parameters]}]
                            (api-response/signature-response
                             (energiatodistus-pdf-service/sign-energiatodistus-pdf
                              db
                              id
                              (:body parameters))
                             (str "Energiatodistus " id)))}}]
      ["/finish"
       {:post {:summary    "Siirrä energiatodistus allekirjoitettu-tilaan"
               :parameters {:path {:id common-schema/Key}}
               :responses  {200 {:body nil}
                            404 {:body schema/Str}}
               :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                             (api-response/signature-response
                              (energiatodistus-service/stop-energiatodistus-signing! db id)
                              (str "Energiatodistus " id)))}}]]]]
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
