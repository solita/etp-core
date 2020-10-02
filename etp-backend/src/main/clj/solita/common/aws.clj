(ns solita.common.aws
  (:require [cognitect.aws.client.api :as aws]
            [solita.etp.config :as config]
            [clojure.tools.logging :as log]))

(defn- invoke [client op request]
  (let [{:keys [Error] :as result} (aws/invoke client {:op      op
                                                       :request request})]
    (if Error
      (log/error "Unable to invoke aws client "
                 (merge {:op op :request request} result))
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
