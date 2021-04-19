(ns solita.etp.api.laskutus
  (:require [clojure.tools.logging :as log]
            [ring.util.response :as r]
            [schema.core :as schema]
            [solita.etp.security :as security]
            [solita.etp.service.laskutus :as laskutus-service]))

(def routes
  [["/laskutus"
    {:middleware [[security/wrap-db-application-name -2]]
     :post {:summary    "Käynnistä laskutusajo"
            :parameters {:query {(schema/optional-key :dryrun) schema/Bool}}
            :responses  {200 {:body nil}}
            :handler    (fn [{{:keys [query]} :parameters :keys [db aws-s3-client]}]
                          (future
                            (try
                              (laskutus-service/do-kuukauden-laskutus
                               db
                               aws-s3-client
                               (:dryrun query))
                             (catch Exception e
                               (log/error "Exception inside laskutus future." e))))
                         (r/response {}))}}]])
