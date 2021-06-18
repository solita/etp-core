(ns solita.etp.api.valvonta-kaytto-toimenpiteet
  (:require
    [ring.util.io :as ring-io]

    [schema.core :as schema]
    [solita.etp.schema.common :as common-schema]
    [solita.etp.schema.valvonta-kaytto :as kaytto-schema]

    [solita.etp.api.response :as api-response]
    [solita.etp.service.rooli :as rooli-service]
    [solita.etp.service.valvonta-kaytto :as valvonta-service]))

(defn toimenpide-404-msg [valvonta-id toimenpide-id]
  (str "Toimenpide " valvonta-id "/" toimenpide-id " does not exists."))

(def routes
  ["/toimenpiteet"
   [""
    {:get  {:summary    "Hae käytönvalvonnan toimenpiteet."
            :parameters {:path {:id common-schema/Key}}
            :responses  {200 {:body [kaytto-schema/Toimenpide]}}
            :access     rooli-service/paakayttaja? 
            :handler    (fn [{{{:keys [id]} :path} :parameters :keys [db]}]
                          (api-response/get-response
                            (valvonta-service/find-toimenpiteet db id)
                            (str "Käytönvalvonta " id " does not exists.")))}

     :post {:summary    "Lisää käytönvalvonnan toimenpide."
            :access     rooli-service/paakayttaja? 
            :parameters {:path {:id common-schema/Key}
                         :body kaytto-schema/ToimenpideAdd}
            :responses  {201 {:body common-schema/Id}
                         404 common-schema/ConstraintError}
            :handler    (fn [{{{:keys [id]} :path :keys [body]}
                              :parameters :keys [db aws-s3-client whoami uri]}]
                          (api-response/with-exceptions
                            #(api-response/created uri
                               (valvonta-service/add-toimenpide! db aws-s3-client whoami id body))
                            [{:constraint :toimenpide-vk-valvonta-id-fkey
                              :response   404}]))}}]
   ["/preview"
    {:conflicting true
     :post        {:summary    "Esikatsele toimenpiteen dokumentti"
                   :parameters {:path {:id common-schema/Key}
                                :body kaytto-schema/ToimenpideAdd}
                   :access     rooli-service/paakayttaja? 
                   :responses  {200 {:body nil}
                                404 {:body schema/Str}}
                   :handler    (fn [{{{:keys [id]} :path :keys [body]}
                                     :parameters :keys [db whoami]}]
                                 (api-response/pdf-response
                                   (ring-io/piped-input-stream
                                     (partial valvonta-service/preview-toimenpide
                                              db whoami id body))
                                   (valvonta-service/toimenpide-filename body)
                                   (str "Käytönvalvonta " id " does not exists.")))}}]
   ["/:toimenpide-id"
    [""
     {:conflicting true
      :get         {:summary    "Hae yksittäisen toimenpiteen tiedot."
                    :parameters {:path {:id            common-schema/Key
                                        :toimenpide-id common-schema/Key}}
                    :responses  {200 {:body kaytto-schema/Toimenpide}
                                 404 {:body schema/Str}}
                    :access     rooli-service/paakayttaja? 
                    :handler    (fn [{{{:keys [id toimenpide-id]} :path}
                                      :parameters :keys [db]}]
                                  (api-response/get-response
                                    (valvonta-service/find-toimenpide db id toimenpide-id)
                                    (toimenpide-404-msg id toimenpide-id)))}

      :put {:summary    "Muuta toimenpiteen tietoja."
            :access     rooli-service/paakayttaja?
            :parameters {:path {:id            common-schema/Key
                                :toimenpide-id common-schema/Key}
                         :body kaytto-schema/ToimenpideUpdate}
            :responses  {200 {:body nil}
                         404 {:body schema/Str}}
            :handler    (fn [{{{:keys [id toimenpide-id]} :path :keys [body]}
                              :parameters :keys [db]}]
                          (api-response/ok|not-found
                            (valvonta-service/update-toimenpide!
                              db id toimenpide-id body)
                            (toimenpide-404-msg id toimenpide-id)))}}]

    ["/document/:filename"
     {:get {:summary    "Esikatsele tai lataa toimenpiteen dokumentti"
            :parameters {:path {:id            common-schema/Key
                                :toimenpide-id common-schema/Key
                                :filename      schema/Str}}
            :access     rooli-service/paakayttaja? 
            :responses  {200 {:body nil}
                         404 {:body schema/Str}}
            :handler    (fn [{{{:keys [id toimenpide-id filename]} :path}
                              :parameters :keys [db whoami aws-s3-client]}]
                          (api-response/pdf-response
                            (ring-io/piped-input-stream
                              (partial valvonta-service/find-toimenpide-document
                                       db aws-s3-client whoami id toimenpide-id))
                            filename
                            (toimenpide-404-msg id toimenpide-id)))}}]]])