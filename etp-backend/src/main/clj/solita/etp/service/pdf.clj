(ns solita.etp.service.pdf
  (:require
    [clojure.java.io :as io]
    [clostache.parser :as clostache])
  (:import
    [com.openhtmltopdf.pdfboxout PdfRendererBuilder PdfRendererBuilder$PdfAConformance]
    [com.openhtmltopdf.svgsupport BatikSVGDrawer]
    (com.openhtmltopdf.outputdevice.helper BaseRendererBuilder$FontStyle)))

(defn- add-font [builder font-resource font-weight font-style]
  (.useFont builder (-> font-resource io/resource io/file) "roboto" (Integer/valueOf font-weight) font-style true))

(defn html->pdf
  [data output-stream]
  (let [html-doc (clostache/render-resource "pdf/base.html" data)
        builder (PdfRendererBuilder.)]
    (.useFastMode builder)
    (.withHtmlContent builder html-doc nil)

    (.usePdfAConformance builder PdfRendererBuilder$PdfAConformance/PDFA_1_A)

    (add-font builder "fonts/Roboto-Bold.ttf" 700 BaseRendererBuilder$FontStyle/NORMAL)
    (add-font builder "fonts/Roboto-BoldItalic.ttf" 700 BaseRendererBuilder$FontStyle/ITALIC)
    (add-font builder "fonts/Roboto-Italic.ttf" 400 BaseRendererBuilder$FontStyle/ITALIC)
    (add-font builder "fonts/Roboto-Regular.ttf" 400 BaseRendererBuilder$FontStyle/NORMAL)

    (.useSVGDrawer builder (BatikSVGDrawer.))

    (.toStream builder output-stream)
    (.run builder)))