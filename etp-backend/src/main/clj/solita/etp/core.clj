(ns solita.etp.core
  (:require [solita.etp.system :as system]))

(def default-config-file "prod.edn")

(defn -main [& args]
  (system/start! (or (first args) default-config-file)))
