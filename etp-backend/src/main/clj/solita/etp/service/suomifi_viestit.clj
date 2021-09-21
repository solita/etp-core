(ns solita.etp.service.suomifi-viestit
  (:require [clostache.parser :as clostache]
            [solita.etp.config :as config]
            [clj-http.client :as http]
            [schema-tools.coerce :as sc]
            [clojure.tools.logging :as log]
            [solita.etp.exception :as exception]
            [solita.common.xml :as xml]
            [schema.core :as schema]
            [clj-http.conn-mgr :as conn-mgr]
            [clojure.string :as str]))

(defn- debug-print [info]
  (when config/suomifi-viestit-debug?
    (log/info info)))

(defn- request-create-xml [data]
  (let [xml (clostache/render-resource (str "suomifi/viesti.xml") data)]
    (debug-print xml)
    xml))

(defn- ^:dynamic make-send-requst! [request]
  (if config/suomifi-viestit-endpoint-url
    (http/post config/suomifi-viestit-endpoint-url
               (cond-> {:body request}
                       config/suomifi-viestit-proxy? (assoc
                                                       :connection-manager
                                                       (conn-mgr/make-socks-proxied-conn-manager "localhost" 1080))))
    (do
      (log/info "Missing suomifi viestit endpoint url. Skip request to suomifi viestit...")
      {:status 200})))

(defn- send-request! [request]
  (try
    (let [response (make-send-requst! request)]
      (if (= 200 (:status response))
        (do
          (debug-print (:body response))
          (:body response))
        (do
          (log/error (str "Sending xml failed with status " (:status response) " " (:body response)))
          (exception/throw-ex-info! :suomifi-viestit-request-failed
                                    (str "Sending xml failed with status " (:status response) " " (:body response))))))
    (catch Exception e
      (log/error "Sending xml failed: " e)
      (exception/throw-ex-info! :suomifi-viestit-request-failed (str "Sending xml failed: " (.getMessage e))))))

(defn- read-response->xml [response]
  (-> response xml/string->xml xml/without-soap-envelope first xml/with-kebab-case-tags))

(defn- trim [s]
  (when s
    (-> s (str/replace #"\s+" " ") str/trim)))

(defn- response-parser [response-soap]
  (let [response-xml (read-response->xml response-soap)]
    (debug-print response-xml)
    {:tila-koodi        (xml/get-content response-xml [:laheta-viesti-result :tila-koodi :tila-koodi])
     :tila-koodi-kuvaus (trim (xml/get-content response-xml [:laheta-viesti-result :tila-koodi :tila-koodi-kuvaus]))
     :sanoma-tunniste   (xml/get-content response-xml [:laheta-viesti-result :tila-koodi :sanoma-tunniste])}))

(defn- log-error [response]
  (when (not= (:tila-koodi response) 202)
    (log/error
      (str "Sending suomifi " (:sanoma-tunniste response)
           " message failed with status " (:tila-koodi response)
           " " (:tila-koodi-kuvaus response)))))

(defn- read-response [response]
  (let [coercer (sc/coercer {:tila-koodi        schema/Int
                             :tila-koodi-kuvaus schema/Str
                             :sanoma-tunniste   schema/Str}
                            sc/string-coercion-matcher)]
    (-> response response-parser coercer log-error)))

(defn send-message! [sanoma
                     kohde
                     & [{:keys [viranomaistunnus palvelutunnus tulostustoimittaja
                                yhteyshenkilo-nimi yhteyshenkilo-email
                                laskutus-tunniste laskutus-salasana
                                paperitoimitus? laheta-tulostukseen?]
                         :or   {viranomaistunnus     config/suomifi-viestit-viranomaistunnus
                                palvelutunnus        config/suomifi-viestit-palvelutunnus
                                tulostustoimittaja   config/suomifi-viestit-tulostustoimittaja
                                yhteyshenkilo-nimi   config/suomifi-viestit-yhteyshenkilo-nimi
                                yhteyshenkilo-email  config/suomifi-viestit-yhteyshenkilo-email
                                laskutus-tunniste    config/suomifi-viestit-laskutus-tunniste
                                laskutus-salasana    config/suomifi-viestit-laskutus-salasana
                                paperitoimitus?      false
                                laheta-tulostukseen? false}}]]
  (let [data {:viranomainen (cond-> {:viranomaistunnus viranomaistunnus
                                     :palvelutunnus    palvelutunnus}
                                    (and yhteyshenkilo-nimi yhteyshenkilo-email) (assoc :yhteyshenkilo {:nimi  yhteyshenkilo-nimi
                                                                                                        :email yhteyshenkilo-email}))
              :sanoma       sanoma
              :kysely       (cond-> {:kohteet              kohde
                                     :tulostustoimittaja   tulostustoimittaja
                                     :paperitoimitus?      false
                                     :laheta-tulostukseen? false}
                                    (and laskutus-tunniste laskutus-salasana) (assoc :laskutus {:tunniste laskutus-tunniste
                                                                                                :salasana laskutus-salasana}))}
        request-xml (request-create-xml data)
        response (send-request! request-xml)]
    (when response
      (read-response response))))