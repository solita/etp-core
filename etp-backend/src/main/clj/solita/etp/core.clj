(ns solita.etp.core
  (:require [solita.etp.system :as system]))

(defn -main []
  (system/start!))
