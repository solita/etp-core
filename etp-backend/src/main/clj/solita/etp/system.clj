(ns solita.etp.system
  (:require [integrant.core :as ig]
            [solita.etp.db]
            [solita.etp.http-server]))

(def config-file-dir "src/main/resources/")

(defn config-file-path [config-file-name]
  (str config-file-dir config-file-name))

;; TODO read env variables and override the config file
(defn load-config [config-file-name]
  (-> (config-file-path config-file-name) slurp ig/read-string))

(defn start! [config-file-name]
  (ig/init (load-config config-file-name)))
