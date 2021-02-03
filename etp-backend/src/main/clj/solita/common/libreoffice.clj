(ns solita.common.libreoffice
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell])
  (:import (java.io IOException)
           (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

;; Recursive delete from https://gist.github.com/olieidel/c551a911a4798312e4ef42a584677397
(defn rm [file]
  (when (.isDirectory file)
    (doseq [file-in-dir (.listFiles file)]
      (rm file-in-dir)))
  (io/delete-file file))

(defn rm-path [path]
  (let [file (.toFile path)]
    (if (.exists file)
      (rm (.toFile path)))))

(defn config-path []
  ;; The configuration can likely be overridden to be somewhere else,
  ;; but this should be a reasonably safe assumption.
  (str (System/getenv "HOME") "/.config/libreoffice/4"))

(defn make-tmpdir []
  (let [attrs (make-array FileAttribute 0)]
    (Files/createTempDirectory "libreoffice-user" attrs)))

(defn populate-tmpdir [path]
  (let [src-dir (config-path)
        cmd ["cp" "-r" src-dir (str path "/config")]
        result (apply shell/sh cmd)]
    (when (not (= 0 (:exit result)))
      (throw (IOException. (:err result))))))

(defn exec-lo [tmpdir args]
  (let [dirparam (str "-env:UserInstallation=file://" tmpdir "/config")]
    (apply shell/sh
           "libreoffice"
           dirparam
           "--headless"
           args)))

(defn run-with-args [& args]
  (let [tmpdir (make-tmpdir)]
    (try
      (populate-tmpdir tmpdir)
      (exec-lo tmpdir args)
      (finally (rm-path tmpdir)))))

