(ns solita.etp.api.energiatodistus-liite
  (:require [ring.util.response :as r]
            [reitit.ring.schema :as reitit-schema]
            [solita.etp.schema.liite :as liite-schema]
            [solita.etp.service.liite :as liite-service]
            [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.api.response :as api-response]
            [cognitect.aws.client.api :as aws]
            [solita.etp.service.rooli :as rooli-service])
  (:import (java.io InputStream)))

(def routes
  ["/liitteet"
   ["/files"
    {:conflicting true
     :post {:summary    "Energiatodistuksen liitteiden lisäys tiedostoista."
            :access     rooli-service/laatija?
            :parameters {:path {:id common-schema/Key}
                         :multipart {:files (schema/conditional
                                              vector? [reitit-schema/TempFilePart]
                                              :else reitit-schema/TempFilePart)}}
            :responses  {201 {:body nil}
                        404 common-schema/ConstraintError}
            :handler    (fn [{{{:keys [id]} :path {:keys [files]} :multipart} :parameters
                           :keys [db aws-s3-client whoami]}]
                       (api-response/response-with-exceptions 201
                         #(liite-service/add-liitteet-from-files
                            db aws-s3-client whoami id (if (vector? files) files [files]))
                         [{:constraint :liite-energiatodistus-id-fkey :response 404}]))}}]

   ["/link"
    {:conflicting true
     :post {:summary    "Liite linkin lisäys energiatodistukseen."
            :access     rooli-service/laatija?
            :parameters {:path {:id common-schema/Key}
                         :body liite-schema/LiiteLinkAdd}
            :responses  {201 {:body nil}
                        404 common-schema/ConstraintError}
            :handler    (fn [{{{:keys [id]} :path :keys [body]} :parameters
                           :keys [db whoami]}]
                       (api-response/response-with-exceptions 201
                         #(liite-service/add-liite-from-link! db whoami id body)
                         [{:constraint :liite-energiatodistus-id-fkey :response 404}]))}}]

   [""
    {:get {:summary "Hae energiatodistuksen liitteet."
           :access  (some-fn rooli-service/laatija? rooli-service/paakayttaja?)
           :parameters {:path {:id common-schema/Key}}
           :responses {200 {:body [liite-schema/Liite]}}
           :handler (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                      (r/response (liite-service/find-energiatodistus-liitteet db id)))}}]

   ["/:liite-id"
    {:conflicting true
     :delete {:summary    "Poista liite energiatodistuksesta."
              :access     (some-fn rooli-service/laatija? rooli-service/paakayttaja?)
              :parameters {:path {:id common-schema/Key
                                  :liite-id common-schema/Key}}
              :responses  {200 {:body nil}
                          404 {:body schema/Str}}
              :handler    (fn [{{{:keys [id liite-id]} :path} :parameters :keys [db]}]
                          (api-response/put-response
                              (liite-service/delete-liite db liite-id)
                              (str "Energiatodistuksen " id " liite " liite-id " does not exists.")))}}]

   ["/:liite-id/content"
    {:get {:summary "Hae energiatodistuksen yhden liitteen sisältö."
           :access  (some-fn rooli-service/laatija? rooli-service/paakayttaja?)
           :parameters {:path {:id common-schema/Key
                               :liite-id common-schema/Key}}
           :responses {200 {:body nil}
                       404 {:body schema/Str}}
           :handler (fn [{{{:keys [id liite-id]} :path} :parameters :keys [db aws-s3-client]}]
                      (let [liite (liite-service/find-energiatodistus-liite-content db aws-s3-client liite-id)]
                        (api-response/file-response
                          (:content liite) (:nimi liite) (:contenttype liite) false
                          (str "Energiatodistuksen " id " liite " liite-id " does not exists."))))}}]])
