(ns solita.etp.service.energiatodistus-pdf
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]))

(defn generate [energiatodistus]
  (-> "dummy.pdf" io/resource io/input-stream))
