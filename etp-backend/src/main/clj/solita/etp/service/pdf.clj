(ns solita.etp.service.pdf
  (:require
    [clojure.java.io :as io]
    [clostache.parser :as clostache])
  (:import
    [com.openhtmltopdf.pdfboxout PdfRendererBuilder PdfRendererBuilder$PdfAConformance]
    [com.openhtmltopdf.svgsupport BatikSVGDrawer]
    (com.openhtmltopdf.outputdevice.helper BaseRendererBuilder$FontStyle)
    (java.io ByteArrayOutputStream)))

(defn- add-font [builder font-resource font-weight font-style]
  (.useFont builder (-> font-resource io/resource io/file) "roboto" (Integer/valueOf font-weight) font-style true))

(defn- render-template-with-content [template data]
  (let [content (clostache/render template data)]
    (clostache/render-resource "pdf/template.html" {:content content})))

(defn html->pdf
  [template data output-stream]
  (let [html-doc (render-template-with-content template data)
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

(defn generate-pdf->bytes [template template-data]
  (with-open [baos (ByteArrayOutputStream.)]
    (html->pdf template template-data baos)
    (.toByteArray baos)))