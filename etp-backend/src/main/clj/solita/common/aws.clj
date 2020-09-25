(ns solita.common.aws
  (:require [cognitect.aws.client.api :as aws]
            [solita.etp.jwt :as jwt]
            [cognitect.aws.credentials :as credentials]))

(def bucket-name "etp-energiatodistuksen-tiedostot-dev")

(defonce s3 (aws/client {:api                  :s3}))

(jwt/http-get "http://169.254.170.2$AWS_CONTAINER_CREDENTIALS_RELATIVE_URI"
          #(-> % println))

(defn put-object [file-name, bytes]
  (aws/invoke s3 {:op      :PutObject
                  :request {:Bucket bucket-name
                            :Key    file-name
                            :Body   bytes}}))

(defn get-object [file-name]
  (:Body (aws/invoke s3 {:op      :GetObject
                         :request {:Bucket bucket-name
                                   :Key    file-name}})))
