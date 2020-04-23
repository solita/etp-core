(ns solita.etp.service.file
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [solita.etp.db :as db])
  (:import (java.io FileInputStream)))

(defn file->byte-array [file]
  (-> file FileInputStream. .readAllBytes))

(defn add-file-from-file [db id file]
  (-> (jdbc/insert! db :file {:id id
                              :filename (.getName file)
                              :content (file->byte-array file)})
      first
      :id))

(defn add-file-from-input-stream [db id filename is]
  (-> (jdbc/insert! db :file {:id id
                              :filename filename
                              :content (.readAllBytes is)})
      first
      :id))

(defn find-file [db id]
  (some-> (jdbc/query db ["SELECT filename, content FROM file WHERE ID = ?" id])
          first
          (update :content io/input-stream)))
