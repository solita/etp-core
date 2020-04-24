(ns solita.etp.service.file
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [solita.etp.db :as db])
  (:import (java.io FileInputStream)))

(defn file->byte-array [file]
  (-> file FileInputStream. .readAllBytes))

(defn add-file-from-bytes [db id filename bytes]
  (-> (jdbc/insert! db :file {:id id :filename filename :content bytes})
      first
      :id))

(defn add-file-from-file [db id file]
  (add-file-from-bytes db id (.getName file) (file->byte-array file)))

(defn add-file-from-input-stream [db id filename is]
  (add-file-from-bytes db id filename (.readAllBytes is)))

(defn find-file [db id]
  (some-> (jdbc/query db ["SELECT filename, content FROM file WHERE ID = ?" id])
          first
          (update :content io/input-stream)))
