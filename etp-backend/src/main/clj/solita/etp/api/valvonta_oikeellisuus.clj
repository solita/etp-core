(ns solita.etp.api.valvonta-oikeellisuus
  (:require [solita.etp.service.rooli :as rooli-service]
            [solita.etp.service.valvonta-oikeellisuus :as valvonta-service]
            [solita.etp.schema.valvonta-oikeellisuus :as oikeellisuus-schema]
            [solita.etp.schema.valvonta :as valvonta-schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.api.response :as api-response]
            [ring.util.response :as r]
            [schema.core :as schema]
            [schema-tools.core :as schema-tools]
            [reitit.ring.schema :as reitit-schema]
            [solita.etp.schema.liite :as liite-schema]))

(def routes
  [["/valvonta/oikeellisuus"
    ["/toimenpidetyypit"
     {:conflicting true
      :get         {:summary   "Hae energiatodistusten oikeellisuuden valvonnan toimenpidetyypit."
                    :responses {200 {:body [common-schema/Luokittelu]}}
                    :access    (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
                    :handler   (fn [{:keys [db]}]
                                 (r/response (valvonta-service/find-toimenpidetyypit db)))}}]
    ["/virhetyypit"
     {:conflicting true
      :get         {:summary   "Hae energiatodistusten virhetyypit."
                    :responses {200 {:body [oikeellisuus-schema/Virhetyyppi]}}
                    :access    (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
                    :handler   (fn [{:keys [db]}]
                                 (r/response (valvonta-service/find-virhetyypit db)))}}]

    ["/severities"
     {:conflicting true
      :get         {:summary   "Hae energiatodistusten virheiden vakavuustaso."
                    :responses {200 {:body [common-schema/Luokittelu]}}
                    :access    (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
                    :handler   (fn [{:keys [db]}]
                                 (r/response (valvonta-service/find-severities db)))}}]

    ["/templates"
     {:conflicting true
      :get         {:summary   "Hae energiatodistusten oikeellisuuden valvonnan asiakirjapohjat."
                    :responses {200 {:body [valvonta-schema/Template]}}
                    :access    (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
                    :handler   (fn [{:keys [db]}]
                                 (r/response (valvonta-service/find-templates db)))}}]

    ["/count"
     {:conflicting true
      :get         {:summary   "Hae energiatodistusten oikeellisuuden valvontojen lukumäärä."
                    :parameters {:query {(schema/optional-key :own) schema/Bool}}
                    :responses {200 {:body {:count schema/Int}}}
                    :handler   (fn [{:keys [db whoami]}]
                                 (r/response (valvonta-service/count-valvonnat db)))}}]

    [""
     {:conflicting true
      :get         {:summary   "Hae energiatodistusten oikeellisuuden valvonnat (työjono)."
                    :parameters {:query {(schema/optional-key :own) schema/Bool
                                         (schema/optional-key :limit) schema/Int
                                         (schema/optional-key :offset) schema/Int}}
                    :responses {200 {:body [oikeellisuus-schema/ValvontaStatus]}}
                    :access    rooli-service/paakayttaja?
                    :handler   (fn [{{:keys [query]} :parameters :keys [db whoami]}]
                                 (r/response (valvonta-service/find-valvonnat db query)))}}]

    ["/:id"
     [""
      {:conflicting true
       :get         {:summary    "Hae yksittäisen valvonnan yleiset tiedot."
                     :parameters {:path {:id common-schema/Key}}
                     :responses  {200 {:body oikeellisuus-schema/Valvonta}}
                     :access     (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
                     :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db whoami]}]
                                   (api-response/get-response
                                     (valvonta-service/find-valvonta db id)
                                     (str "Energiatodistus " id " does not exists.")))}

       :put         {:summary    "Muuta valvonnan yleisiä tietoja."
                     :access     rooli-service/paakayttaja?
                     :parameters {:path {:id common-schema/Key}
                                  :body (schema-tools/optional-keys-schema oikeellisuus-schema/ValvontaSave)}
                     :responses  {200 {:body nil}}
                     :handler    (fn [{{{:keys [id]} :path :keys [body]}
                                       :parameters :keys [db whoami]}]
                                   (api-response/ok|not-found
                                     (valvonta-service/save-valvonta! db id body)
                                     (str "Energiatodistus " id " does not exists.")))}}]

     ["/toimenpiteet"
      [""
       {:get  {:summary    "Hae energiatodistuksen valvontatoimenpiteet."
               :parameters {:path {:id common-schema/Key}}
               :responses  {200 {:body [(dissoc oikeellisuus-schema/Toimenpide :virheet)]}}
               :access     (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
               :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db whoami]}]
                             (api-response/get-response
                               (valvonta-service/find-toimenpiteet db whoami id)
                               (str "Energiatodistus " id " does not exists.")))}

        :post {:summary    "Lisää energiatodistuksen valvontatoimenpide."
               :access     (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
               :parameters {:path {:id common-schema/Key}
                            :body oikeellisuus-schema/ToimenpideAdd}
               :responses  {201 {:body common-schema/Id}
                            404 common-schema/ConstraintError}
               :handler    (fn [{{{:keys [id]} :path :keys [body]}
                                 :parameters :keys [db whoami]}]
                             (api-response/with-exceptions
                               #(api-response/created
                                  (str "/valvonta/oikeellisuus/" id "/toimenpiteet")
                                  (valvonta-service/add-toimenpide! db whoami id body))
                               [{:constraint :toimenpide-energiatodistus-id-fkey
                                 :response   404}]))}}]
      ["/:toimenpide-id"
       [""
       {:get {:summary    "Hae yksittäisen toimenpiteen tiedot."
              :parameters {:path {:id            common-schema/Key
                                  :toimenpide-id common-schema/Key}}
              :responses  {200 {:body oikeellisuus-schema/Toimenpide}
                           404 {:body schema/Str}}
              :access     (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
              :handler    (fn [{{{:keys [id toimenpide-id]} :path}
                                :parameters :keys [db whoami]}]
                            (api-response/get-response
                              (valvonta-service/find-toimenpide
                                db whoami id toimenpide-id)
                              (str "Toimenpide " id "/" toimenpide-id " does not exists.")))}

        :put {:summary    "Muuta toimenpiteen tietoja."
              :access     (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
              :parameters {:path {:id            common-schema/Key
                                  :toimenpide-id common-schema/Key}
                           :body oikeellisuus-schema/ToimenpideUpdate}
              :responses  {200 {:body nil}
                           404 {:body schema/Str}}
              :handler    (fn [{{{:keys [id toimenpide-id]} :path :keys [body]}
                                :parameters :keys [db whoami]}]
                            (api-response/ok|not-found
                              (valvonta-service/update-toimenpide!
                                db whoami id toimenpide-id body)
                              (str "Toimenpide " id "/" toimenpide-id " does not exists.")))}}]
       ["/liitteet"
        [""
        {:get {:summary    "Hae toimenpiteen liitteet."
               :parameters {:path {:id            common-schema/Key
                                   :toimenpide-id common-schema/Key}}
               :responses  {200 {:body [liite-schema/Liite]}
                            404 {:body schema/Str}}
               :access     (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
               :handler    (fn [{{{:keys [id toimenpide-id]} :path}
                                 :parameters :keys [db whoami]}]
                             (api-response/get-response
                               (valvonta-service/find-toimenpide-liitteet db whoami id toimenpide-id)
                               (str "Energiatodistus " id " does not exists.")))}}]
        ["/files"
         {:conflicting true
          :post {:summary    "Toimenpiteen liitteiden lisäys tiedostoista."
                 :access     (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
                 :parameters {:path      {:id            common-schema/Key
                                          :toimenpide-id common-schema/Key}
                              :multipart {:files (schema/conditional
                                                   vector? [reitit-schema/TempFilePart]
                                                   :else reitit-schema/TempFilePart)}}
                 :responses  {201 {:body [common-schema/Key]}
                              404 common-schema/ConstraintError}
                 :handler    (fn [{{{:keys [id toimenpide-id]} :path {:keys [files]} :multipart} :parameters
                                   :keys [db aws-s3-client whoami]}]
                               (api-response/response-with-exceptions
                                 201
                                 #(valvonta-service/add-liitteet-from-files!
                                     db aws-s3-client whoami id toimenpide-id
                                     (if (vector? files) files [files]))
                                 [{:constraint :liite-energiatodistus-id-fkey :response 404}
                                  {:constraint :liite-vo-toimenpide-id-fkey :response 404}]))}}]

        ["/link"
         {:conflicting true
          :post {:summary    "Liite linkin lisäys toimenpiteesee."
                 :access     (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
                 :parameters {:path      {:id            common-schema/Key
                                          :toimenpide-id common-schema/Key}
                              :body liite-schema/LiiteLinkAdd}
                 :responses  {201 {:body common-schema/Id}
                              404 common-schema/ConstraintError}
                 :handler    (fn [{{{:keys [id toimenpide-id]} :path :keys [body]} :parameters :keys [db whoami]}]
                               (api-response/with-exceptions
                                 #(api-response/created
                                    (str "valvonta/oikeellisuus/" id "/toimenpiteet/" toimenpide-id "/liitteet")
                                    {:id (valvonta-service/add-liite-from-link! db whoami id toimenpide-id body)})
                                 [{:constraint :liite-energiatodistus-id-fkey :response 404}
                                  {:constraint :liite-vo-toimenpide-id-fkey :response 404}]))}}]

        ["/:liite-id"
         {:conflicting true
          :delete {:summary    "Poista toimenpiteen liite."
                   :access     (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
                   :parameters {:path {:id common-schema/Key
                                       :toimenpide-id common-schema/Key
                                       :liite-id common-schema/Key}}
                   :responses  {200 {:body nil}
                                404 {:body schema/Str}}
                   :handler    (fn [{{{:keys [id toimenpide-id liite-id]} :path}
                                           :parameters
                                     :keys [db whoami]}]
                                 (api-response/ok|not-found
                                   (valvonta-service/delete-liite! db whoami id toimenpide-id liite-id)
                                   (str "Toimenpiteen " id "/"toimenpide-id " liite " liite-id " does not exists.")))}}]]
       ["/publish"
        {:post {:summary "Tarkista ja julkaise toimenpideluonnos"
                :parameters {:path {:id common-schema/Key
                                    :toimenpide-id common-schema/Key}}
                :access (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
                :responses {200 {:body nil}
                            404 {:body schema/Str}}
                :handler (fn [{{{:keys [id toimenpide-id]} :path} :parameters :keys [db whoami]}]
                           (api-response/ok|not-found
                             (valvonta-service/publish-toimenpide! db whoami id toimenpide-id)
                             (str "Toimenpide " id "/" toimenpide-id " does not exists.")))}}]]]]]])
