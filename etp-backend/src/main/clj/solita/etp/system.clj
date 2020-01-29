(ns solita.etp.system
  (:require [integrant.core :as ig]
            [solita.etp.db]))

(def config-file-path "src/main/resources/config.edn")

;; TODO read env variables and override the config file
(defn load-config []
  (-> config-file-path slurp ig/read-string))

(defn start! []
  (ig/init (load-config)))
