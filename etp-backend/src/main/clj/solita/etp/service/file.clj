(ns solita.etp.service.file
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [solita.etp.db :as db])
  (:import (java.io FileInputStream)))

; *** Require sql functions ***
(db/require-queries 'file)

(defn file->byte-array [file]
  (-> file FileInputStream. .readAllBytes))

(defn upsert-file-from-bytes [db id filename bytes]
  (:id (file-db/upsert-file<! db {:id id :filename filename :content bytes})))

(defn upsert-file-from-file [db id file]
  (upsert-file-from-bytes db id (.getName file) (file->byte-array file)))

(defn upsert-file-from-input-stream [db id filename is]
  (upsert-file-from-bytes db id filename (.readAllBytes is)))

(defn find-file [db id]
  (some-> (file-db/select-file db {:id id})
          first
          (update :content io/input-stream)))
