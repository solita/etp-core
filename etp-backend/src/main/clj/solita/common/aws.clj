(ns solita.common.aws
  (:require [cognitect.aws.client.api :as aws]
            [cognitect.aws.credentials :as credentials]
            [solita.etp.config :as config]
            [clojure.tools.logging :as log]))

(defonce bucket (config/env "FILES_BUCKET_NAME" "files"))

(defonce s3 (aws/client {:api                  :s3
                         :credentials-provider (credentials/basic-credentials-provider
                                                 {:access-key-id     "minio"
                                                  :secret-access-key "minio123"})
                         :endpoint-override    {:protocol :http
                                                :hostname "localhost"
                                                :port     9000}}))

(defn- invoke [{:keys [op request]}]
  (let [{:keys [Error] :as result} (aws/invoke s3 {:op      op
                                                   :request request})]
    (if Error
      (log/error (:message Error))
      result)))

(defn put-object [{:keys [key filename content]}]
  (invoke {:op      :PutObject
           :request {:Bucket   bucket
                     :Key      key
                     :Body     content
                     :Metadata {:filename filename}}}))

(defn get-object [key]
  (when-let [result (invoke {:op      :GetObject
                             :request {:Bucket bucket
                                       :Key    key}})]
    {:content (:Body result) :filename (-> result :Metadata :filename)}))