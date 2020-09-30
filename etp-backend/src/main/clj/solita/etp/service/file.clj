(ns solita.etp.service.file
  (:require [clojure.java.io :as io]
            [solita.common.aws :as aws])
  (:import (java.io FileInputStream)))

(defn file->byte-array [file]
  (-> file FileInputStream. .readAllBytes))

(defn upsert-file-from-bytes [aws-s3-client key filename bytes]
  (aws/put-object aws-s3-client key filename bytes))

(defn upsert-file-from-file [aws-s3-client key file]
  (upsert-file-from-bytes aws-s3-client key (.getName file) (file->byte-array file)))

(defn upsert-file-from-input-stream [aws-s3-client key filename is]
  (upsert-file-from-bytes aws-s3-client key filename (.readAllBytes is)))

(defn find-file [aws-s3-client key]
  (some-> (aws/get-object aws-s3-client key)
          (update :content io/input-stream)))
