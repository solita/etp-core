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

(defn put-object [{:keys [key content] :as d}]
  (let [result
        (aws/invoke s3 {:op      :PutObject
                        :request {:Bucket bucket
                                  :Key    key
                                  :Body   content}})]
    (clojure.pprint/pprint result)))

(defn get-object [key]
  {:content (:Body (aws/invoke s3 {:op      :GetObject
                                   :request {:Bucket bucket
                                             :Key    key}}))})

; :cognitect.anomalies/category :cognitect.anomalies/not-found