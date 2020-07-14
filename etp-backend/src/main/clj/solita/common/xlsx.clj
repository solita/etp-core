(ns solita.common.xlsx
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import (org.apache.poi.ss.usermodel WorkbookFactory)
           (org.apache.poi.ss.util CellAddress)
           (org.apache.poi.xssf.usermodel XSSFSimpleShape)))

;;
;; Reading
;;

(defn load-xlsx [^java.io.InputStream is]
  (WorkbookFactory/create is))

(defn get-sheet [xlsx idx]
  (.getSheetAt xlsx idx))

(defn row-and-column-idx [address]
  (let [cell-address (CellAddress. address)]
    {:row-idx (.getRow cell-address)
     :col-idx (.getColumn cell-address)}))

(defn get-row [sheet idx]
  (.getRow sheet idx))

(defn get-cell [row idx]
  (.getCell row idx))

(defn get-cell-value [cell]
  (let [v (.getStringCellValue cell)]
    (if (str/blank? v) nil v)))

(defn get-cell-value-at [sheet address]
  (let [{:keys [row-idx col-idx]} (row-and-column-idx address)]
    (-> (get-row sheet row-idx)
        (get-cell col-idx)
        (get-cell-value))))

;;
;; Writing
;;

(defn create-xlsx []
  (WorkbookFactory/create (boolean true)))

(defn save-xlsx [xlsx path]
  (with-open [os (io/output-stream path)]
    (.write xlsx os)))

(defn create-sheet [xlsx label]
  (.createSheet xlsx label))

(defn create-row [sheet idx]
  (.createRow sheet idx))

(defn create-cell [row idx]
  (.createCell row idx))

(defn cell-value [v]
  (cond
    (instance? java.time.LocalDate v) v
    (number? v) (double v)
    :else (str v)))

(defn set-cell-value [cell v]
  (->> v cell-value (.setCellValue cell)))

(defn set-cell-value-at [sheet address v]
  (let [{:keys [row-idx col-idx]} (row-and-column-idx address)]
    (-> (get-row sheet row-idx)
        (get-cell col-idx)
        (set-cell-value v))))

(defn create-cell-with-value [row idx v]
  (let [cell (create-cell row idx)]
    (set-cell-value cell v)
    cell))

(defn create-bold-font [xlsx]
  (doto (.createFont xlsx)
    (.setBold true)))

(defn create-style-with-font [xlsx font]
  (doto (.createCellStyle xlsx)
    (.setFont font)))

(defn create-style-with-format [xlsx format]
  (doto (.createCellStyle xlsx)
    (.setDataFormat (-> xlsx
                        .getCreationHelper
                        .createDataFormat
                        (.getFormat format)))))

(defn set-column-width [sheet idx width]
  (.setColumnWidth sheet idx width))

(defn replace-footer-text [sheet match replacement]
  (doseq [shape (-> sheet .getDrawingPatriarch .getShapes .iterator iterator-seq)]
    (when-let [footer (and (instance? XSSFSimpleShape shape)
                           (re-find match (.getText shape))
                           (-> shape .getTextParagraphs first .getTextRuns first))]
      (let [footer-text (str/replace (.getText shape) match replacement)]
        (.setText footer footer-text)))))