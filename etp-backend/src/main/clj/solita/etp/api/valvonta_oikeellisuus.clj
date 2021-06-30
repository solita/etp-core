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

(defn toimenpide-404-msg [valvonta-id toimenpide-id]
  (api-response/msg-404 "toimenpide" valvonta-id toimenpide-id))

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
      :get         {:summary    "Hae energiatodistusten oikeellisuuden valvontojen lukumäärä."
                    :access    (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
                    :parameters {:query valvonta-schema/ValvontaQuery}
                    :responses  {200 {:body {:count schema/Int}}}
                    :handler    (fn [{{:keys [query]} :parameters :keys [db whoami]}]
                                  (r/response (valvonta-service/count-valvonnat db whoami query)))}}]

    [""
     {:conflicting true
      :get         {:summary   "Hae energiatodistusten oikeellisuuden valvonnat (työjono)."
                    :parameters {:query (merge valvonta-schema/ValvontaQuery
                                               valvonta-schema/ValvontaQueryWindow)}
                    :responses {200 {:body [oikeellisuus-schema/ValvontaStatus]}}
                    :access    (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
                    :handler   (fn [{{:keys [query]} :parameters :keys [db whoami]}]
                                 (r/response (valvonta-service/find-valvonnat db whoami query)))}}]

    ["/:id"
     [""
      {:conflicting true
       :get         {:summary    "Hae yksittäisen valvonnan yleiset tiedot."
                     :parameters {:path {:id common-schema/Key}}
                     :responses  {200 {:body oikeellisuus-schema/Valvonta}}
                     :access     (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
                     :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                                   (api-response/get-response
                                     (valvonta-service/find-valvonta db id)
                                     (str "Energiatodistus " id " does not exists.")))}

       :put         {:summary    "Muuta valvonnan yleisiä tietoja."
                     :access     rooli-service/paakayttaja?
                     :parameters {:path {:id common-schema/Key}
                                  :body (schema-tools/optional-keys-schema oikeellisuus-schema/ValvontaSave)}
                     :responses  {200 {:body nil}}
                     :handler    (fn [{{{:keys [id]} :path :keys [body]}
                                       :parameters :keys [db]}]
                                   (api-response/ok|not-found
                                     (valvonta-service/save-valvonta! db id body)
                                     (api-response/msg-404 "energiatodistus" id)))}}]

     ["/toimenpiteet"
      [""
       {:get  {:summary    "Hae energiatodistuksen valvontatoimenpiteet."
               :parameters {:path {:id common-schema/Key}}
               :responses  {200 {:body [(dissoc oikeellisuus-schema/Toimenpide :virheet)]}}
               :access     (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
               :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db whoami]}]
                             (api-response/get-response
                               (valvonta-service/find-toimenpiteet db whoami id)
                               (api-response/msg-404 "energiatodistus" id)))}

        :post {:summary    "Lisää energiatodistuksen valvontatoimenpide."
               :access     (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
               :parameters {:path {:id common-schema/Key}
                            :body oikeellisuus-schema/ToimenpideAdd}
               :responses  {201 {:body common-schema/Id}
                            404 common-schema/ConstraintError}
               :handler    (fn [{{{:keys [id]} :path :keys [body]}
                                 :parameters :keys [db aws-s3-client whoami uri]}]
                             (api-response/with-exceptions
                               #(api-response/created uri
                                                      (valvonta-service/add-toimenpide! db aws-s3-client whoami id body))
                               [{:constraint :toimenpide-energiatodistus-id-fkey
                                 :response   404}]))}}]
      ["/preview"
       {:conflicting true
        :post        {:summary    "Esikatsele toimenpiteen dokumentti"
                      :parameters {:path {:id common-schema/Key}
                                   :body oikeellisuus-schema/ToimenpideAdd}
                      :access     (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
                      :responses  {200 {:body nil}
                                   404 {:body schema/Str}}
                      :handler    (fn [{{{:keys [id]} :path :keys [body]}
                                        :parameters :keys [db whoami]}]
                                    (api-response/pdf-response
                                      (valvonta-service/preview-toimenpide db whoami id body)
                                      (valvonta-service/toimenpide-filename body)
                                      (api-response/msg-404 "energiatodistus" id)))}}]
      ["/:toimenpide-id"
       [""
        {:conflicting true
         :get         {:summary    "Hae yksittäisen toimenpiteen tiedot."
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
                                       (toimenpide-404-msg id toimenpide-id)))}
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
                              (toimenpide-404-msg id toimenpide-id)))}}]
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
          :post {:summary    "Liite linkin lisäys toimenpiteeseen."
                 :access     (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
                 :parameters {:path      {:id            common-schema/Key
                                          :toimenpide-id common-schema/Key}
                              :body liite-schema/LiiteLinkAdd}
                 :responses  {201 {:body common-schema/Id}
                              404 common-schema/ConstraintError}
                 :handler    (fn [{{{:keys [id toimenpide-id]} :path :keys [body]} :parameters :keys [db whoami uri]}]
                               (api-response/with-exceptions
                                 #(api-response/created uri
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
        {:post {:summary    "Tarkista ja julkaise toimenpideluonnos"
                :parameters {:path {:id            common-schema/Key
                                    :toimenpide-id common-schema/Key}}
                :access     (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
                :responses  {200 {:body nil}
                             404 {:body schema/Str}}
                :handler    (fn [{{{:keys [id toimenpide-id]} :path} :parameters :keys [db whoami aws-s3-client]}]
                              (api-response/ok|not-found
                                (valvonta-service/publish-toimenpide! db aws-s3-client whoami id toimenpide-id)
                                (toimenpide-404-msg id toimenpide-id)))}}]
       ["/document/:filename"
        {:get {:summary    "Esikatsele tai lataa toimenpiteen dokumentti"
               :parameters {:path {:id            common-schema/Key
                                   :toimenpide-id common-schema/Key
                                   :filename      schema/Str}}
               :access     (some-fn rooli-service/paakayttaja? rooli-service/laatija?)
               :responses  {200 {:body nil}
                            404 {:body schema/Str}}
               :handler    (fn [{{{:keys [id toimenpide-id filename]} :path}
                                 :parameters :keys [db whoami aws-s3-client]}]
                             (api-response/pdf-response
                               (valvonta-service/find-toimenpide-document
                                 db aws-s3-client whoami id toimenpide-id)
                               filename
                               (toimenpide-404-msg id toimenpide-id)))}}]]]
     ["/notes"
      [""
       {:get  {:summary    "Hae energiatodistuksen valvonnan muistiinpanot."
               :parameters {:path {:id common-schema/Key}}
               :responses  {200 {:body [oikeellisuus-schema/Note]}}
               :access     rooli-service/paakayttaja?
               :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                             (r/response (valvonta-service/find-notes db id)))}

        :post {:summary    "Lisää energiatodistuksen valvontamuistiinpano."
               :access     rooli-service/paakayttaja?
               :parameters {:path {:id common-schema/Key}
                            :body schema/Str}
               :responses  {201 {:body common-schema/Id}
                            404 common-schema/ConstraintError}
               :handler    (fn [{{{:keys [id]} :path :keys [body]}
                                 :parameters :keys [db uri]}]
                             (api-response/with-exceptions
                               #(api-response/created
                                  uri (valvonta-service/add-note! db id body))
                               [{:constraint :vo-note-energiatodistus-id-fkey
                                 :response   404}]))}}]
      ["/:note-id"
       [""
        {:put {:summary    "Muuta muistiinpanoa."
               :access     rooli-service/paakayttaja?
               :parameters {:path {:id      common-schema/Key
                                   :note-id common-schema/Key}
                            :body schema/Str}
               :responses  {200 {:body nil}
                            404 {:body schema/Str}}
               :handler    (fn [{{{:keys [id note-id]} :path :keys [body]}
                                 :parameters :keys [db]}]
                             (api-response/ok|not-found
                               (valvonta-service/update-note! db note-id body)
                               (str "Muistiinpano " id "/" note-id " does not exists.")))}}]]]]]])
