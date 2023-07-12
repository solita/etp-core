(ns solita.etp.test-api
  (:require [solita.etp.handler :as handler]
            [solita.etp.test-system :as ts]))

(defn handler
  "Mimics real handler usage with test assets"
  [req]
  (handler/handler (merge req {:db ts/*db*
                               :aws-s3-client ts/*aws-s3-client*})))
