(ns solita.etp.service.asha
  (:require [clostache.parser :as clostache]
            [clj-http.conn-mgr :as conn-mgr]
            [solita.etp.schema.asha :as asha-schema]
            [clj-http.client :as http]
            [solita.common.xml :as xml]
            [schema-tools.coerce :as sc]
            [clojure.tools.logging :as log]
            [solita.etp.config :as config]
            [solita.etp.exception :as exception]
            [clojure.data.codec.base64 :as b64]))

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

(defn- ^:dynamic make-send-requst! [request]
  (http/post config/asha-endpoint-url
             (cond-> {:content-type "application/xop+xml;charset=\"UTF-8\"; type=\"text/xml\""
                      :body         request}
                     config/asha-proxy? (assoc
                                          :connection-manager
                                          (conn-mgr/make-socks-proxied-conn-manager "localhost" 1080)))))

(defn- send-request! [request]
  (try
    (let [response (make-send-requst! request)]
      (if (= 200 (:status response))
        (do
          (debug-print (:body response))
          (:body response))
        (do
          (log/error (str "Sending xml failed with status " (:status response) " " (:body response)))
          (exception/throw-ex-info! :asha-request-failed
                                    (str "Sending xml failed with status " (:status response) " " (:body response))))))
    (catch Exception e
      (log/error "Sending xml failed: " e)
      (exception/throw-ex-info! :asha-request-failed (str "Sending xml failed: " (.getMessage e))))))

(defn- request-handler [data resource parser-fn schema]
  (let [request-xml (request-create-xml resource data)
        response-xml (send-request! request-xml)]
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

(defn response-parser-action-info [response-soap]
  (let [response-xml (read-response->xml response-soap)
        action-info (fn [path]
                      {:object-class         (xml/get-content response-xml (vec (concat [:return :action-info-response] path [:object-identity :object-class])))
                       :id                   (xml/get-content response-xml (vec (concat [:return :action-info-response] path [:object-identity :id])))
                       :version              (xml/get-content response-xml (vec (concat [:return :action-info-response] path [:object-identity :version])))
                       :contacting-direction (xml/get-content response-xml (vec (concat [:return :action-info-response] path [:contacting-direction])))
                       :name                 (xml/get-content response-xml (vec (concat [:return :action-info-response] path [:name])))
                       :description          (xml/get-content response-xml (vec (concat [:return :action-info-response] path [:description])))
                       :status               (xml/get-content response-xml (vec (concat [:return :action-info-response] path [:status])))
                       :created              (xml/get-content response-xml (vec (concat [:return :action-info-response] path [:created])))})]
    (debug-print response-xml)
    {:processing-action (action-info [:processing-action])
     :assignee          (xml/get-content response-xml [:return :action-info-response :assignee])
     :queue             (xml/get-content response-xml [:return :action-info-response :queue])
     :selected-decision {:decision               (xml/get-content response-xml [:return :action-info-response :selected-decision :decision])
                         :next-processing-action (action-info [:selected-decision :next-processing-action])}}))

(defn open-case! [case]
  (-> (request-handler case "case-create" response-parser-case-create asha-schema/CaseCreateResponse) :case-number))

(defn execute-operation! [data & [response-parser schema]]
  (request-handler data "execute-operation" response-parser schema))

(defn case-info [sender-id request-id case-number]
  (execute-operation! {:request-id request-id
                      :sender-id   sender-id
                      :case-info   {:case-number case-number}}
                      response-parser-case-info
                      asha-schema/CaseInfoResponse))

(defn action-info [sender-id request-id case-number processing-action-name]
  (execute-operation! {:request-id            request-id
                      :sender-id              sender-id
                      :processing-action-info {:case-number   case-number
                                               :processing-action-name-identity processing-action-name}}
                      response-parser-action-info
                      asha-schema/ActionInfoResponse))

(defn proceed-operation! [sender-id request-id case-number processing-action decision]
  (execute-operation! {:request-id request-id
                       :sender-id  sender-id
                       :identity   (cond-> {:case {:number case-number}}
                                           processing-action (assoc :processing-action {:name-identity processing-action}))
                       :proceed-operation {:decision decision}}))

(defn attach-contact-to-processing-action! [sender-id request-id case-number processing-action contact]
  (execute-operation! {:sender-id sender-id
                       :request-id request-id
                       :identity   {:case              {:number case-number}
                                    :processing-action {:name-identity processing-action}}
                       :attach     {:contact contact}}))

(defn bytes->base64-string [bytes]
  (String. (b64/encode bytes) "UTF-8"))

(defn add-documents-to-processing-action! [sender-id request-id case-number processing-action documents]
  (execute-operation! {:sender-id sender-id
                      :request-id request-id
                      :identity   {:case              {:number case-number}
                                   :processing-action {:name-identity processing-action}}
                      :attach     {:document (map (fn [document]
                                                    (update document :content bytes->base64-string))
                                                  documents)}}))

(defn resolve-case-processing-action-state [sender-id request-id case-number]
  (->>  ["Vireillepano" "Käsittely" "Päätöksenteko"]
       (map (fn [processing-action]
              (try
                {processing-action (-> (action-info sender-id request-id case-number processing-action) :processing-action :status)}
                (catch Exception _e))))
        (into (array-map))))

(defn move-processing-action [sender-id request-id case-number processing-action]
  (when-let [action (cond
                        (= processing-action "Käsittely") {:processing-action "Vireillepano"
                                                           :decision          "Siirry käsittelyyn"}
                        (= processing-action "Päätöksenteko") {:processing-action "Käsittely"
                                                               :decision          "Siirry päätöksentekoon"})]
    (when (not (get (resolve-case-processing-action-state sender-id request-id case-number) processing-action))
      (proceed-operation! sender-id request-id case-number (:processing-action action) (:decision action)))))

(defn mark-processing-action-as-ready [sender-id request-id case-number processing-action]
  (proceed-operation! sender-id request-id case-number processing-action "Valmis"))

(defn close-case! [sender-id request-id case-number]
  (let [latest-prosessing-action (->> (resolve-case-processing-action-state sender-id request-id case-number)
                                      keys
                                      last)]
    (proceed-operation! sender-id request-id case-number latest-prosessing-action "Sulje asia")))

(defn take-processing-action! [sender-id request-id case-number processing-action]
  (execute-operation! {:sender-id        sender-id
                       :request-id       request-id
                       :identity         {:case {:number case-number}
                                          :processing-action {:name-identity processing-action}}
                       :start-processing {:assignee sender-id}}))

(defn kayttaja->contact [kayttaja]
  {:type          "ORGANIZATION"                            ;No enum constant fi.ys.eservice.entity.ContactType.PERSON
   :first-name    (:etunimi kayttaja)
   :last-name     (:sukunimi kayttaja)
   :email-address (:email kayttaja)})