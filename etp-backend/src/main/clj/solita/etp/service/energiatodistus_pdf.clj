(ns solita.etp.service.energiatodistus-pdf
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.tools.logging :as log]
            [solita.common.xlsx :as xlsx]))

(def xlsx-template-path "energiatodistus-template.xlsx")
(def sheet-count 8)
(def tmp-dir "tmp/")

(def mappings {0 {"K7" [:perustiedot :nimi]
                  "K8" [:perustiedot :katuosoite-fi]

                  ;; TODO needs luokittelu for postitoimipaikka
                  "K9" #(str (-> % :perustiedot :postinumero) " " "Helsinki")
                  "K12" [:perustiedot :rakennustunnus]
                  "K13" [:perustiedot :valmistumisvuosi]

                  ;; TODO find alakayttotarkoitukset from db
                  "K14" [:perustiedot :kayttotarkoitus]
                  "K16" [:id]

                  ;; TODO checkboxes D19-D21
                  ;; TODO format date
                  "M21" [:perustiedot :havainnointikaynti]
                  ;; TODO M36 and M37 E-luku
                  ;; TODO laatija B42

                  "J42" [:perustiedot :yritys :nimi]

                  ;; TODO real todistuksen laatimispäivä with correct formatting
                  "B50" (fn [_] (str (java.time.LocalDate/now)))
                  ;; TODO voimassaolopäivä K50
                  }})

(defn fill-xlsx-template [energiatodistus]
  (with-open [is (-> xlsx-template-path io/resource io/input-stream)]
    (let [loaded-xlsx (xlsx/load-xlsx is)
          sheets (map #(xlsx/get-sheet loaded-xlsx %) (range sheet-count))
          path (->> (java.util.UUID/randomUUID)
                    .toString
                    (format "energiatodistus-%s.xlsx")
                    (str tmp-dir))]
      (doseq [[sheet sheet-mappings] mappings]
        (doseq [[cell cursor-or-f] sheet-mappings]
          (xlsx/set-cell-value-at (nth sheets sheet)
                                  cell
                                  (if (vector? cursor-or-f)
                                    (str (get-in energiatodistus cursor-or-f))
                                    (cursor-or-f energiatodistus)))))
      (io/make-parents path)
      (xlsx/save-xlsx loaded-xlsx path)
      path)))

;; Uses current Libreoffice export settings. Make sure they are set
;; for PDFA-2B.
(defn xlsx->pdf [path]
  "Uses current LibreOffice export settings. Make sure they are set
   to PDFA-2B. Path must be a path on disk, not on classpath."
  (let [file (io/file path)
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
      (str/replace path #".xlsx$" ".pdf")
      (do (log/error "XLSX to PDF conversion failed" sh-result)
        (throw (ex-info "XLSX to PDF conversion failed" sh-result))))))

(defn generate [energiatodistus]
  (let [xlsx-path (fill-xlsx-template energiatodistus)
        pdf-path (xlsx->pdf xlsx-path)
        is (io/input-stream pdf-path)]
    (io/delete-file xlsx-path)
    (io/delete-file pdf-path)
    is))
