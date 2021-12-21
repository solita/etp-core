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
            [clojure.data.codec.base64 :as b64]
            [clojure.string :as str]))

(defn bytes->base64 [bytes]
  (String. (b64/encode bytes) "UTF-8"))

(defn- request-create-xml [resource data]
  (clostache/render-resource (str "asha/" resource ".xml") data))

(defn response->xml [response]
  (-> response xml/string->xml xml/without-soap-envelope first xml/with-kebab-case-tags))

(defn- ^:dynamic post! [request]
  (log/debug request)
  (if config/asha-endpoint-url
    (http/post config/asha-endpoint-url
               (cond-> {:content-type     "application/xop+xml;charset=\"UTF-8\"; type=\"text/xml\""
                        :throw-exceptions false
                        :body             request}
                       config/asha-proxy? (assoc
                                            :connection-manager
                                            (conn-mgr/make-socks-proxied-conn-manager "localhost" 1080))))
    (do
      (log/info "Missing asha endpoint url. Skip request to asha...")
      {:status 200})))

(defn- throw-ex-info! [request response cause]
  (throw
    (ex-info
      (str "Sending asiahallinta request failed.")
      {:type         :asha-request-failed
       :endpoint-url config/asha-endpoint-url
       :request
       (select-keys request [:request-id :sender-id
                             :name :description :identity
                             :case-info :processing-action-info
                             :proceed-operation])
       :response     response
       :cause        (ex-data cause)}
      cause)))

(defn- assert-status! [response]
  (when-not (#{200 201 202 203 204 205 206 207 300 301 302 303 304 307} (:status response))
    (exception/illegal-argument! (str "Invalid response status: " (:status response))))
  response)

(defn- read-response [request response response-reader coerce-response!]
  (try
    (some-> response assert-status! :body response->xml response-reader coerce-response!)
    (catch Throwable t (throw-ex-info! request response t))))

(defn- send-request! [request request-template response-reader schema]
  (try
    (let [request-xml (request-create-xml request-template request)
          response (post! request-xml)]
      (when (some? response-reader)
        (read-response request response response-reader
                       (sc/coercer schema sc/string-coercion-matcher))))
    (catch Throwable t (throw-ex-info! request nil t))))

(defn read-response-case-create [response-xml]
  {:id          (xml/get-content response-xml [:return :object-identity :id])
   :case-number (xml/get-content response-xml [:return :case-number])})

(defn read-response-case-info [response-xml]
  {:id             (xml/get-content response-xml [:return :case-info-response :object-identity :id])
   :case-number    (xml/get-content response-xml [:return :case-info-response :case-number])
   :status         (xml/get-content response-xml [:return :case-info-response :status])
   :classification (xml/get-content response-xml [:return :case-info-response :classification :code])
   :name           (xml/get-content response-xml [:return :case-info-response :name])
   :description    (xml/get-content response-xml [:return :case-info-response :description])
   :created        (xml/get-content response-xml [:return :case-info-response :created])})

(defn read-action-info-action [path response-xml]
  {:object-class         (xml/get-content response-xml (vec (concat [:return :action-info-response] path [:object-identity :object-class])))
   :id                   (xml/get-content response-xml (vec (concat [:return :action-info-response] path [:object-identity :id])))
   :version              (xml/get-content response-xml (vec (concat [:return :action-info-response] path [:object-identity :version])))
   :contacting-direction (xml/get-content response-xml (vec (concat [:return :action-info-response] path [:contacting-direction])))
   :name                 (xml/get-content response-xml (vec (concat [:return :action-info-response] path [:name])))
   :description          (xml/get-content response-xml (vec (concat [:return :action-info-response] path [:description])))
   :status               (xml/get-content response-xml (vec (concat [:return :action-info-response] path [:status])))
   :created              (xml/get-content response-xml (vec (concat [:return :action-info-response] path [:created])))})

(defn read-response-action-info [response-xml]
  {:processing-action (read-action-info-action [:processing-action] response-xml)
   :assignee          (xml/get-content response-xml [:return :action-info-response :assignee])
   :queue             (xml/get-content response-xml [:return :action-info-response :queue])
   :selected-decision {:decision
                       (xml/get-content response-xml [:return :action-info-response :selected-decision :decision])
                       :next-processing-action
                       (read-action-info-action [:selected-decision :next-processing-action] response-xml)}})

(defn open-case! [case]
  (-> (send-request! case "case-create" read-response-case-create asha-schema/CaseCreateResponse) :case-number))

(defn execute-operation! [data & [response-reader schema]]
  (send-request! data "execute-operation" response-reader schema))

(defn case-info [sender-id request-id case-number]
  (execute-operation! {:request-id request-id
                       :sender-id  sender-id
                       :case-info  {:case-number case-number}}
                      read-response-case-info
                      asha-schema/CaseInfoResponse))

(defn action-info [sender-id request-id case-number processing-action-name]
  (execute-operation! {:request-id             request-id
                       :sender-id              sender-id
                       :processing-action-info {:case-number                     case-number
                                                :processing-action-name-identity processing-action-name}}
                      read-response-action-info
                      asha-schema/ActionInfoResponse))

(defn proceed-operation! [sender-id request-id case-number processing-action decision]
  (execute-operation! {:request-id        request-id
                       :sender-id         sender-id
                       :identity          (cond-> {:case {:number case-number}}
                                                  processing-action (assoc :processing-action {:name-identity processing-action}))
                       :proceed-operation {:decision decision}}))

(defn attach-contact-to-processing-action! [sender-id request-id case-number processing-action contact]
  (execute-operation! {:sender-id  sender-id
                       :request-id request-id
                       :identity   {:case              {:number case-number}
                                    :processing-action {:name-identity processing-action}}
                       :attach     {:contact contact}}))

(defn add-documents-to-processing-action! [sender-id request-id case-number processing-action documents]
  (execute-operation! {:sender-id  sender-id
                       :request-id request-id
                       :identity   {:case              {:number case-number}
                                    :processing-action {:name-identity processing-action}}
                       :attach     {:document documents}}))

(defn resolve-case-processing-action-state [sender-id request-id case-number]
  (->> ["Vireillepano" "Käsittely" "Päätöksenteko"]
       (map (fn [processing-action]
              (try
                {processing-action (-> (action-info sender-id request-id case-number processing-action) :processing-action :status)}
                (catch Exception _e))))
       (into (array-map))))

(defn resolve-latest-case-processing-action-state [sender-id request-id case-number]
  (->> (resolve-case-processing-action-state sender-id request-id case-number)
       keys
       last))

(defn move-processing-action! [sender-id request-id case-number processing-action]
  (when-let [action (cond
                      (= processing-action "Käsittely") {:processing-action "Vireillepano"
                                                         :decision          "Siirry käsittelyyn"}
                      (= processing-action "Päätöksenteko") {:processing-action "Käsittely"
                                                             :decision          "Siirry päätöksentekoon"})]
    (when (not (get (resolve-case-processing-action-state sender-id request-id case-number) processing-action))
      (proceed-operation! sender-id request-id case-number (:processing-action action) (:decision action)))))

(defn mark-processing-action-as-ready! [sender-id request-id case-number processing-action]
  (proceed-operation! sender-id request-id case-number processing-action "Valmis"))

(defn take-processing-action! [sender-id request-id case-number processing-action]
  (execute-operation! {:sender-id        sender-id
                       :request-id       request-id
                       :identity         {:case              {:number case-number}
                                          :processing-action {:name-identity processing-action}}
                       :start-processing {:assignee sender-id}}))

(defn log-toimenpide! [sender-id request-id case-number processing-action & [documents]]
  (move-processing-action!
    sender-id
    request-id
    case-number
    (-> processing-action :identity :processing-action :name-identity))
  (take-processing-action!
    sender-id
    request-id
    case-number
    (-> processing-action :identity :processing-action :name-identity))

  (execute-operation! {:request-id        request-id
                       :sender-id         sender-id
                       :identity          (:identity processing-action)
                       :processing-action (:processing-action processing-action)})

  (doseq [document documents]
    (add-documents-to-processing-action!
      sender-id
      request-id
      case-number
      (-> processing-action :processing-action :name)
      [{:content (bytes->base64 document)
        :type    (-> processing-action :document :type)
        :name    (-> processing-action :document :filename)}]))
  (take-processing-action! sender-id request-id case-number (-> processing-action :processing-action :name))
  (mark-processing-action-as-ready!
    sender-id
    request-id
    case-number
    (-> processing-action :processing-action :name)))

(defn close-case! [sender-id request-id case-number description]
  (let [latest-prosessing-action (resolve-latest-case-processing-action-state sender-id request-id case-number)]
    (when description
      (log-toimenpide!
        sender-id
        request-id
        case-number
        {:identity          {:case              {:number case-number}
                             :processing-action {:name-identity latest-prosessing-action}}
         :processing-action {:name           "Asian sulkeminen"
                             :reception-date (java.time.Instant/now)
                             :description    description}}))
    (proceed-operation! sender-id request-id case-number latest-prosessing-action "Sulje asia")))

(defn string-join [separator coll]
  (str/join separator (->> coll
                           (map str)
                           (remove empty?))))