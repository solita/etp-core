(ns solita.etp.service.energiatodistus-pdf
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [solita.common.xlsx :as xlsx]))

(def xlsx-template-path "src/main/resources/energiatodistus-template.xlsx")
(def sheet-count 8)
(def temp-dir "tmp/")

(defn fill-xlsx-template [energiatodistus]
  (let [loaded-xlsx (xlsx/load-xlsx xlsx-template-path)
        sheets (map #(xlsx/get-sheet loaded-xlsx %) (range sheet-count))
        file-path (->> (java.util.UUID/randomUUID)
                      .toString
                      (format "energiatodistus-%s.xlsx")
                      (str temp-dir))]
    (xlsx/set-cell-value-at (nth sheets 0)
                            "K7"
                            (-> energiatodistus :perustiedot :nimi))
    (xlsx/set-cell-value-at (nth sheets 0) "K8" "Esimerkkikatu 1 A 1")
    (xlsx/set-cell-value-at (nth sheets 0) "K9" "33100 Tampere")
    (io/make-parents file-path)
    (xlsx/save-xlsx loaded-xlsx file-path)
    file-path))

(defn generate [energiatodistus]
  (fill-xlsx-template energiatodistus)

  ;; TODO write pdf instead of using dummy
  (-> "dummy.pdf" io/resource io/input-stream))
