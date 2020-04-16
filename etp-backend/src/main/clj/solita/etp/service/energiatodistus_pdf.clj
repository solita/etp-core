(ns solita.etp.service.energiatodistus-pdf
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.tools.logging :as log]
            [solita.common.xlsx :as xlsx]))

(def xlsx-template-path "src/main/resources/energiatodistus-template.xlsx")
(def sheet-count 8)
(def tmp-dir "tmp/")

(defn fill-xlsx-template [energiatodistus]
  (let [loaded-xlsx (xlsx/load-xlsx xlsx-template-path)
        sheets (map #(xlsx/get-sheet loaded-xlsx %) (range sheet-count))
        file-path (->> (java.util.UUID/randomUUID)
                      .toString
                      (format "energiatodistus-%s.xlsx")
                      (str tmp-dir))]
    (xlsx/set-cell-value-at (nth sheets 0)
                            "K7"
                            (-> energiatodistus :perustiedot :nimi))
    (xlsx/set-cell-value-at (nth sheets 0) "K8" "Esimerkkikatu 1 A 1")
    (xlsx/set-cell-value-at (nth sheets 0) "K9" "33100 Tampere")
    (io/make-parents file-path)
    (xlsx/save-xlsx loaded-xlsx file-path)
    file-path))

;; Uses current Libreoffice export settings. Make sure they are set
;; for PDFA-2B

(defn xlsx->pdf [file-path]
  (let [file (io/file file-path)
        filename (.getName file)
        dir (.getParent file)
        {:keys [exit err] :as sh-result} (shell/sh "libreoffice"
                                                   "--headless"
                                                   "--convert-to"
                                                   "pdf"
                                                   filename
                                                   :dir
                                                   dir)]
    (if (and (zero? exit) (str/blank? err))
      (str/replace file-path #".xlsx$" ".pdf")
      (do (log/error "XLSX to PDF conversion failed" sh-result)
        (throw (ex-info "XLSX to PDF conversion failed" sh-result))))))

(defn generate [energiatodistus]
  (let [xlsx-file-path (fill-xlsx-template energiatodistus)
        pdf-file-path (xlsx->pdf xlsx-file-path)])

  ;; TODO write pdf instead of using dummy
  (-> "dummy.pdf" io/resource io/input-stream))
