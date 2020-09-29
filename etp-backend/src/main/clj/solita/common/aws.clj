(ns solita.common.aws
  (:require [cognitect.aws.client.api :as aws]
            [solita.etp.jwt :as jwt]
            [cognitect.aws.credentials :as credentials]))

(def bucket "etp-energiatodistuksen-tiedostot-dev")

(defonce s3 (aws/client {:api                  :s3
                         :credentials-provider (credentials/basic-credentials-provider
                                                 {:access-key-id     "AKIAIOSFODNN7EXAMPLE"
                                                  :secret-access-key "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"})
                         :endpoint-override    {:protocol :http
                                                :hostname "localhost"
                                                :port     9000}}))

(defn put-object [{:keys [filename content]}]
  (aws/invoke s3 {:op      :PutObject
                  :request {:Bucket bucket
                            :Key    filename
                            :Body   content}}))

(defn get-object [filename]
  {:content (:Body (aws/invoke s3 {:op      :GetObject
                                   :request {:Bucket bucket
                                             :Key    filename}}))})
