(ns solita.etp.service.file
  (:require [clojure.java.io :as io]
            [solita.common.aws :as aws])
  (:import (clojure.lang ExceptionInfo)
           (java.io File FileInputStream)))

(defn file->byte-array [^File file]
  (-> file FileInputStream. .readAllBytes))

(defn upsert-file-from-bytes [aws-s3-client key bytes]
  (aws/put-object aws-s3-client key bytes))

(defn upsert-file-from-file [aws-s3-client key file]
  (upsert-file-from-bytes aws-s3-client key (file->byte-array file)))

(defn upsert-file-from-input-stream [aws-s3-client key is]
  (upsert-file-from-bytes aws-s3-client key (.readAllBytes is)))

(defn find-file [aws-s3-client key]
  (some-> (aws/get-object aws-s3-client key)
          io/input-stream))

(defn file-exists? [aws-s3-client key]
  (try
    (aws/get-object-head aws-s3-client key)
    true
    (catch ExceptionInfo e
      (let [{:keys [type]} (ex-data e)]
        (if (= type :resource-not-found)
          false
          (throw e))))))