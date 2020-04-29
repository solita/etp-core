(ns solita.etp.service.energiatodistus-xlsx
  (:require [clojure.java.io :as io]
            [solita.common.xlsx :as xlsx]
            [solita.etp.service.energiatodistus :as energiatodistus-service]))

(def tmp-dir "tmp/")

(def mappings [])

(defn fill-headers [sheet]
  (let [row (xlsx/create-row sheet 0)]
    (xlsx/create-cell-with-value row 0 "ABC")
    (xlsx/create-cell-with-value row 1 "Testing")))

(defn find-energiatodistus-xlsx [db id]
  (when-let [energiatodistus
             (energiatodistus-service/find-energiatodistus db id)]
    (let [path (->> (java.util.UUID/randomUUID)
                    .toString
                    (format "energiatodistus-%s.xlsx")
                    (str tmp-dir))
          xlsx (xlsx/create-xlsx)
          sheet (xlsx/create-sheet xlsx "Energiatodistus")
          _ (fill-headers sheet)
          _ (io/make-parents path)
          _ (xlsx/save-xlsx xlsx path)
          is (io/input-stream path)]
      (io/delete-file path)
      is)))
