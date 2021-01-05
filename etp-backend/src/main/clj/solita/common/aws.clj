(ns solita.common.aws
  (:require [cognitect.aws.client.api :as aws]
            [clojure.tools.logging :as log]
            [solita.etp.exception :as exception]))

(def anomalies->etp-codes
  {:cognitect.anomalies/forbidden   :forbidden              ;http status code: 403
   :cognitect.anomalies/not-found   :not-found              ;http status code: 404
   :cognitect.anomalies/busy        :busy                   ;http status code: 503
   :cognitect.anomalies/unavailable :unavailable})          ;http status code: 504

(defn- invoke [client op request]
  (let [result (aws/invoke client {:op      op
                                   :request request})]
    (if (contains? result :cognitect.anomalies/category)
      (do
        (log/error "Unable to invoke aws client "
                   (merge {:op op :request request} result))
        (exception/throw-ex-info! (-> result :cognitect.anomalies/category anomalies->etp-codes)
                                  (or (-> result :Error :Message) (:cognitect.anomalies/message result))))
      result)))

(defn put-object [{:keys [client bucket]} key filename content]
  (invoke client
          :PutObject
          {:Bucket   bucket
           :Key      key
           :Body     content
           :Metadata {:filename filename}}))

(defn get-object [{:keys [client bucket]} key]
  (when-let [result (invoke client
                            :GetObject
                            {:Bucket bucket
                             :Key    key})]
    {:content (:Body result) :filename (-> result :Metadata :filename)}))
