(ns solita.etp.service.file
  (:require [clojure.java.io :as io]
            [solita.common.aws :as aws])
  (:import (java.io FileInputStream)))

(defn file->byte-array [file]
  (-> file FileInputStream. .readAllBytes))

(defn upsert-file-from-bytes [key filename bytes]
  (aws/put-object {:key key :filename filename :content bytes}))

(defn upsert-file-from-file [key file]
  (upsert-file-from-bytes key (.getName file) (file->byte-array file)))

(defn upsert-file-from-input-stream [key filename is]
  (upsert-file-from-bytes key filename (.readAllBytes is)))

(defn find-file [key]
  (some-> (aws/get-object key)
          (update :content io/input-stream)))
