(ns solita.etp.service.json
  (:require [jsonista.core :as json]))

(def mapper (json/object-mapper {:decode-key-fn keyword }))

(defn read-value [json] (json/read-value json mapper))

(def write-value-as-string json/write-value-as-string)