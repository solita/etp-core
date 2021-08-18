(ns solita.etp.api.valvonta-kaytto
  (:require [ring.util.response :as r]
            [schema.core :as schema]
            [schema-tools.core :as schema-tools]
            [reitit.ring.schema :as reitit-schema]
            [solita.etp.api.response :as api-response]
            [solita.etp.api.valvonta-kaytto-toimenpiteet :as toimenpiteet-api]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.service.valvonta-kaytto :as valvonta-service]
            [solita.etp.schema.valvonta :as valvonta-schema]
            [solita.etp.schema.valvonta-kaytto :as valvonta-kaytto-schema]
            [solita.etp.schema.liite :as liite-schema]
            [clojure.java.io :as io]))

(def routes
  [["/valvonta/kaytto"
    ["/ilmoituspaikat"
     {:conflicting true
      :get         {:summary   "Hae käytönvalvonnan ilmoituspaikat."
                    :responses {200 {:body [common-schema/Luokittelu]}}
                    :access    rooli-service/paakayttaja?
                    :handler   (fn [{:keys [db]}]
                                 (r/response (valvonta-service/find-ilmoituspaikat db)))}}]
    ["/roolit"
     {:conflicting true
      :get         {:summary   "Hae käytönvalvonnan roolit."
                    :responses {200 {:body [common-schema/Luokittelu]}}
                    :access    rooli-service/paakayttaja?
                    :handler   (fn [{:keys [db]}]
                                 (r/response (valvonta-service/find-roolit db)))}}]
    ["/toimitustavat"
     {:conflicting true
      :get         {:summary   "Hae käytönvalvonnan toimitustavat."
                    :responses {200 {:body [common-schema/Luokittelu]}}
                    :access    rooli-service/paakayttaja?
                    :handler   (fn [{:keys [db]}]
                                 (r/response (valvonta-service/find-toimitustavat db)))}}]
    ["/toimenpidetyypit"
     {:conflicting true
      :get         {:summary   "Hae käytönvalvonnan toimenpidetyypit."
                    :responses {200 {:body [common-schema/Luokittelu]}}
                    :access    rooli-service/paakayttaja?
                    :handler   (fn [{:keys [db]}]
                                 (r/response (valvonta-service/find-toimenpidetyypit db)))}}]
    ["/templates"
     {:conflicting true
      :get         {:summary   "Hae käytönvalvonnan asiakirjapohjat."
                    :responses {200 {:body [valvonta-schema/Template]}}
                    :access    rooli-service/paakayttaja?
                    :handler   (fn [{:keys [db]}]
                                 (r/response (valvonta-service/find-templates db)))}}]
    ["/count"
     {:conflicting true
      :get         {:summary    "Hae käytönvalvontojen lukumäärä."
                    :parameters {:query valvonta-schema/ValvontaQuery}
                    :responses  {200 {:body {:count schema/Int}}}
                    :access     rooli-service/paakayttaja?
                    :handler    (fn [{{:keys [query]} :parameters :keys [db whoami]}]
                                  (r/response (valvonta-service/count-valvonnat db whoami query)))}}]
    [""
     {:conflicting true
      :get         {:summary    "Hae käytönvalvonnat (työjono)."
                    :parameters {:query (merge valvonta-schema/ValvontaQuery
                                               valvonta-schema/ValvontaQueryWindow)}
                    :responses  {200 {:body [valvonta-kaytto-schema/ValvontaStatus]}}
                    :access     rooli-service/paakayttaja?
                    :handler    (fn [{{:keys [query]} :parameters :keys [db whoami]}]
                                  (r/response (valvonta-service/find-valvonnat db whoami query)))}
      :post        {:summary    "Luo uusi käytönvalvonta"
                    :access     rooli-service/paakayttaja?
                    :parameters {:body valvonta-kaytto-schema/ValvontaSave}
                    :responses  {200 {:body common-schema/Id}}
                    :handler    (fn [{{:keys [body]} :parameters :keys [db uri]}]
                                  (api-response/created
                                   uri
                                   {:id (valvonta-service/add-valvonta! db body)}))}}]
    ["/:id"
     [""
      {:conflicting true
       :get         {:summary    "Hae yksittäisen käytönvalvonnan yleiset tiedot."
                     :parameters {:path {:id common-schema/Key}}
                     :responses  {200 {:body valvonta-kaytto-schema/Valvonta}}
                     :access     rooli-service/paakayttaja?
                     :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                                   (api-response/get-response
                                    (valvonta-service/find-valvonta db id)
                                    (str "Käytönvalvonta " id " does not exist.")))}
       :put         {:summary    "Muuta käytönvalvonnan yleisiä tietoja."
                     :access     rooli-service/paakayttaja?
                     :parameters {:path {:id common-schema/Key}
                                  :body valvonta-kaytto-schema/ValvontaSave}
                     :responses  {200 {:body nil}}
                     :handler    (fn [{{{:keys [id]} :path :keys [body]} :parameters :keys [db]}]
                                   (api-response/ok|not-found
                                    (valvonta-service/update-valvonta! db id body)
                                    (str "Käytönvalvonta " id " does not exist.")))}
       :delete    {:summary    "Poista käytönvalvonta"
                   :access     rooli-service/paakayttaja?
                   :parameters {:path {:id common-schema/Key}}
                   :responses  {200 {:body nil}}
                   :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                                 (api-response/ok|not-found
                                  (valvonta-service/delete-valvonta! db id)
                                  (str "Käytönvalvonta " id " does not exist.")))}}]
     ["/henkilot"
      [""
       {:get  {:summary    "Hae käytönvalvonnan henkilöt"
               :parameters {:path {:id common-schema/Key}}
               :responses  {200 {:body [valvonta-kaytto-schema/HenkiloStatus]}}
               :access     rooli-service/paakayttaja?
               :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                             (api-response/get-response
                              (valvonta-service/find-henkilot db id)
                              (str "Käytönvalvonta " id " does not exist.")))}

        :post {:summary    "Lisää käytönvalvontaan henkilö"
               :access     rooli-service/paakayttaja?
               :parameters {:path {:id common-schema/Key}
                            :body valvonta-kaytto-schema/HenkiloSave}
               :responses  {201 {:body common-schema/Id}
                            404 common-schema/ConstraintError}
               :handler    (fn [{{{:keys [id]} :path :keys [body]} :parameters :keys [db uri]}]
                             (api-response/with-exceptions
                               #(api-response/created
                                 uri
                                 {:id (valvonta-service/add-henkilo! db id body)})
                               [{:constraint :henkilo-valvonta-id-fkey
                                 :response 404}]))}}]
      ["/:henkilo-id"
       [""
        {:get {:summary    "Hae yksittäisen henkilön tiedot."
               :parameters {:path {:id common-schema/Key
                                   :henkilo-id common-schema/Key}}
               :responses  {200 {:body valvonta-kaytto-schema/Henkilo}
                            404 {:body schema/Str}}
               :access     rooli-service/paakayttaja?
               :handler    (fn [{{{:keys [id henkilo-id]} :path} :parameters :keys [db]}]
                             (api-response/get-response
                              (valvonta-service/find-henkilo db henkilo-id)
                              (str "Henkilö " id "/" henkilo-id " does not exist.")))}
         :put {:summary    "Muuta henkilön tietoja."
               :access     rooli-service/paakayttaja?
               :parameters {:path {:id common-schema/Key
                                   :henkilo-id common-schema/Key}
                            :body valvonta-kaytto-schema/HenkiloSave}
               :responses  {200 {:body nil}
                            404 {:body schema/Str}}
               :handler    (fn [{{{:keys [id henkilo-id]} :path :keys [body]} :parameters :keys [db]}]
                             (api-response/ok|not-found
                              (valvonta-service/update-henkilo! db id henkilo-id body)
                              (str "Henkilö " id "/" henkilo-id " does not exist.")))}
         :delete {:summary    "Poista henkilö."
                  :access     rooli-service/paakayttaja?
                  :parameters {:path {:id common-schema/Key
                                      :henkilo-id common-schema/Key}}
                  :responses  {200 {:body nil}}
                  :handler    (fn [{{{:keys [id henkilo-id]} :path} :parameters :keys [db]}]
                                (api-response/ok|not-found
                                 (valvonta-service/delete-henkilo! db id henkilo-id)
                                 (str "Henkilö " id "/" henkilo-id " does not exist.")))}}]]]
     ["/yritykset"
      [""
       {:get  {:summary    "Hae käytönvalvonnan yritykset"
               :parameters {:path {:id common-schema/Key}}
               :responses  {200 {:body [valvonta-kaytto-schema/YritysStatus]}}
               :access     rooli-service/paakayttaja?
               :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                             (api-response/get-response
                              (valvonta-service/find-yritykset db id)
                              (str "Käytönvalvonta " id " does not exist.")))}

        :post {:summary    "Lisää käytönvalvontaan yritys"
               :access     rooli-service/paakayttaja?
               :parameters {:path {:id common-schema/Key}
                            :body valvonta-kaytto-schema/YritysSave}
               :responses  {201 {:body common-schema/Id}
                            404 common-schema/ConstraintError}
               :handler    (fn [{{{:keys [id]} :path :keys [body]} :parameters :keys [db uri]}]
                             (api-response/with-exceptions
                               #(api-response/created
                                 uri
                                 {:id (valvonta-service/add-yritys! db id body)})
                               [{:constraint :yritys-valvonta-id-fkey
                                 :response 404}]))}}]
      ["/:yritys-id"
       [""
        {:get {:summary    "Hae yksittäisen yrityksen tiedot."
               :parameters {:path {:id common-schema/Key
                                   :yritys-id common-schema/Key}}
               :responses  {200 {:body valvonta-kaytto-schema/Yritys}
                            404 {:body schema/Str}}
               :access     rooli-service/paakayttaja?
               :handler    (fn [{{{:keys [id yritys-id]} :path} :parameters :keys [db]}]
                             (api-response/get-response
                              (valvonta-service/find-yritys db yritys-id)
                              (str "Yritys " id "/" yritys-id " does not exist.")))}
         :put {:summary    "Muuta yrityksen tietoja."
               :access     rooli-service/paakayttaja?
               :parameters {:path {:id common-schema/Key
                                   :yritys-id common-schema/Key}
                            :body valvonta-kaytto-schema/YritysSave}
               :responses  {200 {:body nil}
                            404 {:body schema/Str}}
               :handler    (fn [{{{:keys [id yritys-id]} :path :keys [body]} :parameters :keys [db]}]
                             (api-response/ok|not-found
                              (valvonta-service/update-yritys! db id yritys-id body)
                              (str "Yritys " id "/" yritys-id " does not exist.")))}
         :delete {:summary    "Poista yritys."
                  :access     rooli-service/paakayttaja?
                  :parameters {:path {:id common-schema/Key
                                      :yritys-id common-schema/Key}}
                  :responses  {200 {:body nil}}
                  :handler    (fn [{{{:keys [id yritys-id]} :path} :parameters :keys [db]}]
                                (api-response/ok|not-found
                                 (valvonta-service/delete-yritys! db id yritys-id)
                                 (str "Yritys " id "/" yritys-id " does not exist.")))}}]]]
     toimenpiteet-api/routes
     ["/liitteet"
      [""
       {:get {:summary    "Hae käytönvalvonnan liitteet."
              :parameters {:path {:id common-schema/Key}}
              :responses  {200 {:body [liite-schema/Liite]}
                           404 {:body schema/Str}}
              :access     rooli-service/paakayttaja?
              :handler    (fn [{{{:keys [id valvonta-id]} :path} :parameters :keys [db]}]
                            (api-response/get-response
                             (valvonta-service/find-liitteet db id)
                             (str "Käytönvalvonta " id " does not exists.")))}}]
      ["/files"
       {:conflicting true
        :post {:summary    "Käytönvalvonnan liitteiden lisäys tiedostoista."
               :access     rooli-service/paakayttaja?
               :parameters {:path {:id common-schema/Key}
                            :multipart {:files (schema/conditional
                                                vector? [reitit-schema/TempFilePart]
                                                :else reitit-schema/TempFilePart)}}
               :responses  {201 {:body [common-schema/Key]}
                            404 common-schema/ConstraintError}
               :handler    (fn [{{{:keys [id]} :path {:keys [files]} :multipart}
                                 :parameters :keys [db aws-s3-client]}]
                             (api-response/response-with-exceptions
                              201
                              #(valvonta-service/add-liitteet-from-files!
                                 db
                                 aws-s3-client
                                 id
                                 (if (vector? files) files [files]))
                              [{:constraint :liite-valvonta-id-fkey :response 404}]))}}]
      ["/link"
       {:conflicting true
        :post {:summary    "Liite-linkin lisäys käytönvalvontaan."
               :access     rooli-service/paakayttaja?
               :parameters {:path {:id common-schema/Key}
                            :body liite-schema/LiiteLinkAdd}
               :responses  {201 {:body common-schema/Id}
                            404 common-schema/ConstraintError}
               :handler    (fn [{{{:keys [id]} :path :keys [body]}
                                 :parameters :keys [db uri]}]
                             (api-response/with-exceptions
                               #(api-response/created
                                 uri
                                 {:id (valvonta-service/add-liite-from-link! db id body)})
                               [{:constraint :liite-valvonta-id-fkey :response 404}]))}}]
      ["/:liite-id"
       [""
        {:conflicting true
         :delete      {:summary    "Poista käytönvalvonnan liite."
                       :access     rooli-service/paakayttaja?
                       :parameters {:path {:id       common-schema/Key
                                           :liite-id common-schema/Key}}
                       :responses  {200 {:body nil}
                                    404 {:body schema/Str}}
                       :handler    (fn [{{{:keys [id liite-id]} :path} :parameters :keys [db]}]
                                     (api-response/ok|not-found
                                       (valvonta-service/delete-liite! db id liite-id)
                                       (str "Liite " id "/" liite-id " does not exist.")))}}]
        ["/:filename"
         {:conflicting true
          :get         {:summary    "Hae käytönvalvonnan liitteen sisältö."
                        :access     rooli-service/paakayttaja?
                        :parameters {:path {:id       common-schema/Key
                                            :liite-id common-schema/Key
                                            :filename schema/Str}}
                        :responses  {200 {:body nil}
                                     404 {:body schema/Str}}
                        :handler    (fn [{{{:keys [id liite-id filename]} :path} :parameters
                                          :keys                                  [db aws-s3-client]}]
                                      (let [{:keys [tempfile contenttype] :as file}
                                            (valvonta-service/find-liite db aws-s3-client id liite-id)]
                                        (if (= (:filename file) filename)
                                          (api-response/file-response
                                            (io/input-stream tempfile) filename contenttype false
                                            (str "Liite " id "/" liite-id " does not exist."))
                                          (api-response/bad-request "Filename is invalid."))))}}]]]]]])
