(ns solita.etp.service.file
  (:require [clojure.java.io :as io]
            [solita.etp.db :as db]
            [solita.common.aws :as aws])
  (:import (java.io FileInputStream)))

; *** Require sql functions ***
(db/require-queries 'file)

(defn file->byte-array [file]
  (-> file FileInputStream. .readAllBytes))

(defn upsert-file-from-bytes [key bytes]
  (aws/put-object {:key key :content bytes}))

(defn upsert-file-from-file [key file]
  (upsert-file-from-bytes key (file->byte-array file)))

(defn upsert-file-from-input-stream [key is]
  (upsert-file-from-bytes key (.readAllBytes is)))

(defn find-file [key]
  (some->> (aws/get-object key) io/input-stream (assoc {} :content)))
