(ns solita.etp.api.energiatodistus
  (:require [ring.util.response :as r]
            [reitit.ring.schema :as reitit-schema]
            [solita.etp.schema.energiatodistus :as energiatodistus-schema]
            [solita.etp.service.energiatodistus :as energiatodistus-service]
            [solita.etp.service.energiatodistus-pdf :as energiatodistus-pdf-service]
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
                             (str "Energiatodistus luonnos " id " does not exists.")))}}]
     ["/pdf"
      {:get {:summary    "Lataa energiatodistus PDF-tiedostona"
             :parameters {:path {:id common-schema/Key}}
             :responses  {200 {:body nil}
                          404 {:body schema/Str}}
             :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db parameters]}]
                           (api-response/pdf-response
                            (energiatodistus-pdf-service/find-energiatodistus-pdf db id)
                            (str "energiatodistus2018-" id ".pdf")
                            (str "Energiatodistus " id " does not exists.")))}}]
     ["/liitteet"
      {:post {:summary "Pohja liitteiden lisäämiseksi energitodistukselle. Ei tee toistaiseksi mitään."
              :parameters {:multipart {:file reitit-schema/TempFilePart}}
              :responses {200 {:body nil}}
              :handler (fn [{{{:keys [file]} :multipart} :parameters}]
                         (println file))}}]
     ["/digest"
      {:get {:summary    "PDF-tiedoston digest allekirjoitusta varten"
             :parameters {:path {:id common-schema/Key}}
             :responses  {200 {:body nil}
                          404 {:body schema/Str}}
              :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db parameters]}]
                            (api-response/get-response
                             (energiatodistus-pdf-service/find-energiatodistus-digest db id)
                             (str "Energiatodistus " id " does not exists.")))}}]]]
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
