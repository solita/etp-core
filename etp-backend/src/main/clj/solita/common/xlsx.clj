(ns solita.common.xlsx
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import (org.apache.poi.ss.usermodel WorkbookFactory)
           (org.apache.poi.ss.util CellAddress)
           (org.apache.poi.xssf.usermodel XSSFWorkbook)))

(defn load-xlsx [path]
  (-> path io/file (WorkbookFactory/create)))

(defn save-xlsx [xlsx path]
  (with-open [os (io/output-stream path)]
    (.write xlsx os)))

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

(defn set-cell-value [cell s]
 (.setCellValue cell s))

(defn set-cell-value-at [sheet address v]
  (let [{:keys [row-idx col-idx]} (row-and-column-idx address)]
    (-> (get-row sheet row-idx)
        (get-cell col-idx)
        (set-cell-value v))))

(defn get-cell-value-at [sheet address]
  (let [{:keys [row-idx col-idx]} (row-and-column-idx address)]
    (-> (get-row sheet row-idx)
        (get-cell col-idx)
        (get-cell-value))))
