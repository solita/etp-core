(ns solita.etp.system
  (:require [integrant.core :as ig]
            [solita.etp.db]))

;; TODO read env variables and override the config file
(defn start! []
  (-> "src/main/resources/config.edn" slurp ig/read-string ig/init))
