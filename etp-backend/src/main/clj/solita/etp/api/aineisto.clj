(ns solita.etp.api.aineisto
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [solita.etp.config :as config]
            [solita.etp.exception :as exception]
            [solita.common.cf-signed-url :as signed-url]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.service.aineisto :as aineisto-service]
            [solita.etp.service.energiatodistus-csv :as energiatodistus-csv]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.api.response :as api-response]
            [solita.etp.api.stream :as api-stream]))

(def aineisto-sources
  {:banks energiatodistus-csv/energiatodistukset-bank-csv})

(defn not-nil-aineisto-source! [x]
  (when (nil? x)
    (throw (ex-info "Expected not-nil aineisto source"
                    {:type :nil-aineisto-source})))
  x)

(def search-exceptions [{:type :nil-aineisto-source :response 404}])

(defn first-address [x-forwarded-for]
  (some-> x-forwarded-for (str/split #"[\s,]+") first))

(def signed-routes
  [["/aineistot"
    ["/:aineisto-id"
     ["/energiatodistukset.csv"
      {:get {:summary "Hae energiatodistusaineisto CSV-tiedostona"
             ;; Note - there is a body, but it is produced through async channel
             :responses {200 {:body nil}}
             :access rooli-service/system?
             :parameters {:path {:aineisto-id common-schema/Key}}
             :handler (fn [{{{:keys [aineisto-id]} :path} :parameters :keys [db whoami] :as request}]
                        (log/info "Producing aineisto" aineisto-id)
                        (api-response/with-exceptions
                          #(let [energiatodistukset-csv (-> aineisto-id
                                                            aineisto-service/aineisto-key
                                                            aineisto-sources
                                                            not-nil-aineisto-source!)
                                 result (energiatodistukset-csv db whoami)]
                             (api-stream/result->async-channel
                              request
                              (merge (api-response/csv-response-headers "energiatodistukset.csv" false)
                                     (api-response/async-cache-headers 86400))
                              result))
                          search-exceptions))}}]]]])

(def external-routes
  [["/aineistot"
    ["/:aineisto-id"
     ["/energiatodistukset.csv"
      {:get {:summary "Hae aineisto"
             :access rooli-service/aineistokayttaja?
             :parameters {:path {:aineisto-id common-schema/Key}}
             :handler (fn [{{{:keys [aineisto-id]} :path} :parameters
                            {:strs [x-forwarded-for]} :headers
                            :keys [db whoami]}]

                        (when (nil? x-forwarded-for)
                          (exception/throw-forbidden!
                           "This functionality is only available behind a reverse proxy"))

                        (when-not (aineisto-service/check-access db (:id whoami) aineisto-id
                                                                 (first-address x-forwarded-for))
                          (exception/throw-forbidden!
                           (str "User " whoami " not permitted to access aineisto " aineisto-id)))

                        (let [url (str config/public-index-url
                                       "/api/signed/aineistot/"
                                       aineisto-id
                                       "/energiatodistukset.csv")
                              expires (+ 60 (signed-url/unix-time))
                              private-key (signed-url/pem-string->private-key config/url-signing-private-key)
                              signing-keys {:key-pair-id config/url-signing-key-id
                                            :private-key private-key}
                              signed-url (signed-url/url->signed-url url
                                                                     expires
                                                                     (first-address x-forwarded-for)
                                                                     signing-keys)]
                          (log/info "Issued" signed-url
                                    "to" (select-keys whoami [:id])
                                    "x-forwarded-for" x-forwarded-for)
                          {:status 302
                           :headers {"Location" signed-url}}))}}]]]])
