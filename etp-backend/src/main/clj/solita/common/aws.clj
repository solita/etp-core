(ns solita.common.aws
  (:require [cognitect.aws.client.api :as aws]
            [solita.etp.config :as config]
            [clojure.tools.logging :as log]))

(defn- invoke [aws-s3-client op request]
  (let [{:keys [Error] :as result} (aws/invoke aws-s3-client {:op      op
                                                              :request request})]
    (if Error
      (log/error "Unable to invoke aws client " (merge {:op op :request request} result))
      result)))

(defn put-object [aws-s3-client key filename content]
  (invoke aws-s3-client
          :PutObject
          {:Bucket   (config/getFilesBucketName)
           :Key      key
           :Body     content
           :Metadata {:filename filename}}))

(defn get-object [aws-s3-client key]
  (when-let [result (invoke aws-s3-client
                            :GetObject
                            {:Bucket (config/getFilesBucketName)
                             :Key    key})]
    {:content (:Body result) :filename (-> result :Metadata :filename)}))