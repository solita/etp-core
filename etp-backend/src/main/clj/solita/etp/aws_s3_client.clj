(ns solita.etp.aws-s3-client
  (:require [integrant.core :as ig]
            [cognitect.aws.client.api :as aws]))

(defmethod ig/init-key :solita.etp/aws-s3-client
  [_ opts]
  (aws/client opts))

(defmethod ig/halt-key! :solita.etp/aws-s3-client
  [_ aws-s3-client]
  (aws/stop aws-s3-client))
