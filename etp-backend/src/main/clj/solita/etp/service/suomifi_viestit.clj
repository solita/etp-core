(ns solita.etp.service.suomifi-viestit
  (:require [clostache.parser :as clostache]
            [solita.etp.config :as config]
            [clj-http.client :as http]
            [schema-tools.coerce :as sc]
            [clojure.tools.logging :as log]
            [solita.common.xml :as xml]
            [schema.core :as schema]
            [clj-http.conn-mgr :as conn-mgr]
            [clojure.string :as str])
  (:import (org.apache.ws.security WSConstants WSEncryptionPart)
           (org.apache.ws.security.components.crypto CryptoFactory)
           (org.apache.ws.security.message WSSecSignature WSSecHeader WSSecTimestamp)
           (java.util Properties)
           (java.io ByteArrayInputStream)
           (org.apache.axis.soap MessageFactoryImpl)
           (org.apache.xml.security.c14n Canonicalizer)
           (org.apache.axis.configuration NullProvider)
           (org.apache.axis.client AxisClient)
           (org.apache.axis MessageContext Message)))

;; register default algorithms
(Canonicalizer/registerDefaultAlgorithms)

(defn- request-create-xml [data]
  (clostache/render-resource (str "suomifi/viesti.xml") data))

(defn- ^:dynamic make-send-request! [request]
  (log/debug request)
  (if config/suomifi-viestit-endpoint-url
    (http/post config/suomifi-viestit-endpoint-url
               (cond-> {:body request}
                       config/suomifi-viestit-proxy? (assoc
                                                       :connection-manager
                                                       (conn-mgr/make-socks-proxied-conn-manager "localhost" 1080))))
    (do
      (log/info "Missing suomifi viestit endpoint url. Skip request to suomifi viestit...")
      {:status 200})))

(defn- raw-request->document [request]
  (with-open [inStream (ByteArrayInputStream. (.getBytes request))]
    (let [engine (AxisClient. (NullProvider.))
          msgContext (MessageContext. engine)
          axisMessage (doto (Message. inStream)
                        (.setMessageContext msgContext))]
      (-> axisMessage .getSOAPEnvelope .getAsDocument))))

(defn- document->signed-request [document]
  (let [c14n (Canonicalizer/getInstance Canonicalizer/ALGO_ID_C14N_WITH_COMMENTS)
        canonicalMessage (.canonicalizeSubtree c14n document)
        in (ByteArrayInputStream. canonicalMessage)]
    (-> (.createMessage (MessageFactoryImpl.) nil in)
        .getSOAPEnvelope .getAsString)))

(defn- signSOAPEnvelope [request keystore-file keystore-password keystore-alias]
  (let [properties (doto (Properties.)
                     (.setProperty "org.apache.ws.security.crypto.merlin.keystore.file" keystore-file)
                     (.setProperty "org.apache.ws.security.crypto.merlin.keystore.password", keystore-password)
                     (.setProperty "org.apache.ws.security.crypto.merlin.keystore.type", "JKS")
                     (.setProperty "signer.username" keystore-alias)
                     (.setProperty "signer.password" keystore-password))
        crypto (CryptoFactory/getInstance properties)
        signer (doto (WSSecSignature.)
                 (.setUserInfo keystore-alias keystore-password)
                 (.setKeyIdentifierType WSConstants/BST_DIRECT_REFERENCE)
                 (.setUseSingleCertificate true))
        doc (raw-request->document request)
        header (doto (WSSecHeader.)
                 (.setMustUnderstand true)
                 (.insertSecurityHeader doc))
        timestamp (doto (WSSecTimestamp.)
                    (.setTimeToLive 60))
        build-doc (.build timestamp doc header)
        timestampPart (WSEncryptionPart. "Timestamp", WSConstants/WSU_NS, "")
        bodyPart (WSEncryptionPart. WSConstants/ELEM_BODY, WSConstants/URI_SOAP11_ENV, "")]
    (.setParts signer [timestampPart bodyPart])
    (document->signed-request (.build signer build-doc crypto header))))

(defn- read-response->xml [response]
  (-> response xml/string->xml xml/without-soap-envelope first xml/with-kebab-case-tags))

(defn- trim [s]
  (when s
    (-> s (str/replace #"\s+" " ") str/trim)))

(defn- response-parser [response-soap]
  (let [response-xml (read-response->xml response-soap)]
    {:tila-koodi        (xml/get-content response-xml [:laheta-viesti-result :tila-koodi :tila-koodi])
     :tila-koodi-kuvaus (trim (xml/get-content response-xml [:laheta-viesti-result :tila-koodi :tila-koodi-kuvaus]))
     :sanoma-tunniste   (xml/get-content response-xml [:laheta-viesti-result :tila-koodi :sanoma-tunniste])}))

(defn- assert-error [response]
  (if (not= (:tila-koodi response) 202)
    (throw (ex-info
             (str "Sending suomifi " (:sanoma-tunniste response)
                  " message failed with status " (:tila-koodi response)
                  " " (:tila-koodi-kuvaus response))
             {:type :suomifi-viestit-attribute-exception
              :data {:sanoma-tunniste   (:sanoma-tunniste response)
                     :tila-koodi        (:tila-koodi response)
                     :tila-koodi-kuvaus (:tila-koodi-kuvaus response)}}))
    response))

(defn- read-response [response]
  (let [coercer (sc/coercer {:tila-koodi        schema/Int
                             :tila-koodi-kuvaus schema/Str
                             :sanoma-tunniste   schema/Str}
                            sc/string-coercion-matcher)]
    (-> response response-parser coercer assert-error)))

(defn- handle-request! [data keystore-file keystore-password keystore-alias]
  (try
    (let [request-xml (request-create-xml data)]
      (if (and keystore-file keystore-password keystore-alias)
        (make-send-request! (signSOAPEnvelope request-xml keystore-file keystore-password keystore-alias))
        (make-send-request! request-xml)))
    (catch Throwable t
      (throw (ex-info (str "Sending suomifi " (-> data :sanoma :tunniste) " message failed with connection error")
                      {:type   :suomifi-viestit-connection-exception
                       :data   {:sanoma-tunniste (-> data :sanoma :tunniste)}}
                      t)))))

(defn- send-request! [data keystore-file keystore-password keystore-alias]
  (some-> (handle-request! data keystore-file keystore-password keystore-alias) :body read-response))

(defn send-message! [sanoma
                     kohde
                     & [{:keys [viranomaistunnus palvelutunnus varmenne tulostustoimittaja
                                yhteyshenkilo-nimi yhteyshenkilo-email
                                laskutus-tunniste laskutus-salasana
                                paperitoimitus? laheta-tulostukseen?
                                keystore-file keystore-password keystore-alias]
                         :or   {viranomaistunnus     config/suomifi-viestit-viranomaistunnus
                                palvelutunnus        config/suomifi-viestit-palvelutunnus
                                varmenne             config/suomifi-viestit-varmenne
                                tulostustoimittaja   config/suomifi-viestit-tulostustoimittaja
                                yhteyshenkilo-nimi   config/suomifi-viestit-yhteyshenkilo-nimi
                                yhteyshenkilo-email  config/suomifi-viestit-yhteyshenkilo-email
                                laskutus-tunniste    config/suomifi-viestit-laskutus-tunniste
                                laskutus-salasana    config/suomifi-viestit-laskutus-salasana
                                paperitoimitus?      config/suomifi-viestit-paperitoimitus?
                                laheta-tulostukseen? config/suomifi-viestit-laheta-tulostukseen?
                                keystore-file        config/suomifi-viestit-keystore-file
                                keystore-password    config/suomifi-viestit-keystore-password
                                keystore-alias       config/suomifi-viestit-keystore-alias}}]]
  (let [data {:viranomainen {:viranomaistunnus viranomaistunnus
                             :palvelutunnus    palvelutunnus
                             :yhteyshenkilo    {:nimi  yhteyshenkilo-nimi
                                                :email yhteyshenkilo-email}}
              :sanoma       (assoc sanoma :varmenne varmenne)
              :kysely       (cond-> {:kohteet              kohde
                                     :tulostustoimittaja   tulostustoimittaja
                                     :paperitoimitus?      paperitoimitus?
                                     :laheta-tulostukseen? laheta-tulostukseen?}
                                    (and (seq laskutus-tunniste) (seq laskutus-salasana))
                                    (assoc :laskutus {:tunniste laskutus-tunniste
                                                      :salasana laskutus-salasana}))}]
    (send-request! data keystore-file keystore-password keystore-alias)))