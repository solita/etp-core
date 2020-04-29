(ns solita.etp.service.energiatodistus-xlsx
  (:require [clojure.java.io :as io]
            [solita.common.xlsx :as xlsx]
            [solita.etp.service.energiatodistus :as energiatodistus-service]))

(def tmp-dir "tmp/")

(def mappings [])

(defn energiatodistus->xlsx [])

(defn find-energiatodistus-xlsx [db id]
  (when-let [energiatodistus
             (energiatodistus-service/find-energiatodistus db id)]
    (let [created-xlsx (xlsx/create-xlsx)
          path (->> (java.util.UUID/randomUUID)
                    .toString
                    (format "energiatodistus-%s.xlsx")
                    (str tmp-dir))
          _ (io/make-parents path)
          _ (xlsx/save-xlsx created-xlsx path)
          is (io/input-stream path)]
      (io/delete-file path)
      is)))
