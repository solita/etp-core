(ns solita.etp.service.energiatodistus-xlsx
  (:require [clojure.java.io :as io]
            [solita.common.xlsx :as xlsx]
            [solita.etp.service.energiatodistus :as energiatodistus-service]))

(def tmp-dir "tmp/")

(def mappings [])

(defn fill-headers [sheet style]
  (let [row (xlsx/create-row sheet 0)]
    (.setRowStyle row style)
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
          bold-font (xlsx/create-bold-font xlsx)
          bold-style (xlsx/create-style xlsx bold-font)
          _ (fill-headers sheet bold-style)
          _ (io/make-parents path)
          _ (xlsx/save-xlsx xlsx path)
          is (io/input-stream path)]
      (io/delete-file path)
      is)))
