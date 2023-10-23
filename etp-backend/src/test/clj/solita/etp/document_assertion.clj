(ns solita.etp.document-assertion
  (:require [clojure.java.io :as io]
            [clojure.test :as t]
            [solita.etp.service.pdf :as pdf]))

(def original-html->pdf pdf/html->pdf)

(defn html->pdf-with-assertion [doc-path-to-compare-to html->pdf-called? html-doc output-stream]
  ;; Mocking the pdf rendering function so that the document contents can be asserted
  ;; Compare the created document to the snapshot
  (t/is (= html-doc
           (slurp (io/resource doc-path-to-compare-to))))
  (reset! html->pdf-called? true)
  ;;Calling original implementation to ensure the functionality doesn't change
  (original-html->pdf html-doc output-stream))
