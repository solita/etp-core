(ns solita.etp.service.asha
  (:require [clostache.parser :as clostache]
            [clj-http.conn-mgr :as conn-mgr]
            [solita.etp.schema.asha :as asha-schema]
            [clj-http.client :as http]
            [solita.common.xml :as xml]
            [schema-tools.coerce :as sc]
            [clojure.tools.logging :as log]
            [clojure.data.codec.base64 :as b64]
            [solita.etp.config :as config]))

(defn debug-print [info]
  (when config/asha-debug?
    (println info)))

(defn- request-create-xml [resource data]
  (let [xml (clostache/render-resource (str "asha/" resource ".xml") data)]
    (debug-print xml)
    xml))

(defn read-response->xml [response]
  (-> response xml/string->xml xml/without-soap-envelope first xml/with-kebab-case-tags))

(defn read-response [response-parser schema]
  (let [coercer (sc/coercer schema sc/string-coercion-matcher)]
    (coercer response-parser)))

(defn- send-request [request]
  (try
    (let [response (http/post config/asha-endpoint-url
                              (cond-> {:content-type   "application/xop+xml;charset=\"UTF-8\"; type=\"text/xml\""
                                       :SOAPAction     ""
                                       :content-length (count request)
                                       :body           request}
                                      config/asha-proxy? (assoc
                                                           :connection-manager
                                                           (conn-mgr/make-socks-proxied-conn-manager "localhost" 1080))))]
      (if (= 200 (:status response))
        (do
          (debug-print (:body response))
          (:body response))
        (do
          (log/error "Sending xml failed with status " (:status response) (:body response))
          (throw (Exception. "Sending xml failed with status " (:status response))))))
    (catch Exception e
      (log/error "Sending xml failed:" e)
      (throw e))))

(defn- request-handler [data resource parser-fn schema]
  (let [request-xml (request-create-xml resource data)
        response-xml (send-request request-xml)]
    (when-let [response-parser (when parser-fn (parser-fn response-xml))]
      (read-response response-parser schema))))

(defn response-parser-case-create [response-soap]
  (let [response-xml (read-response->xml response-soap)]
    (debug-print response-xml)
    {:id          (xml/get-content response-xml [:return :object-identity :id])
     :case-number (xml/get-content response-xml [:return :case-number])}))

(defn response-parser-case-info [response-soap]
  (let [response-xml (read-response->xml response-soap)]
    (debug-print response-xml)
    {:id             (xml/get-content response-xml [:return :case-info-response :object-identity :id])
     :case-number    (xml/get-content response-xml [:return :case-info-response :case-number])
     :status         (xml/get-content response-xml [:return :case-info-response :status])
     :classification (xml/get-content response-xml [:return :case-info-response :classification :code])
     :name           (xml/get-content response-xml [:return :case-info-response :name])
     :description    (xml/get-content response-xml [:return :case-info-response :description])
     :created        (xml/get-content response-xml [:return :case-info-response :created])}))

(defn case-create [case]
  (request-handler case "case-create" response-parser-case-create asha-schema/CaseCreateResponse))

(defn execute-operation [data & [response-parser schema]]
  (request-handler data "execute-operation" response-parser schema))
