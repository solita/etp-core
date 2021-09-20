(ns solita.etp.service.suomifi-viestit
  (:require [clostache.parser :as clostache]
            [clojure.data.codec.base64 :as b64]
            [solita.etp.schema.valvonta-kaytto :as kaytto-schema]
            [solita.etp.service.valvonta-kaytto.toimenpide :as toimenpide]
            [solita.etp.config :as config]
            [clj-http.client :as http]
            [schema-tools.coerce :as sc]
            [clojure.tools.logging :as log]
            [solita.etp.exception :as exception]
            [solita.common.xml :as xml]
            [schema.core :as schema]
            [clojure.java.io :as io]
            [solita.etp.service.pdf :as pdf]
            [clj-http.conn-mgr :as conn-mgr]
            [clojure.string :as str])
  (:import (java.time Instant)
           (java.io ByteArrayOutputStream File)))

(def lahettaja {:nimi             "Asumisen rahoitus- ja kehittämiskeskus"
                :jakeluosoite     "Vesijärvenkatu 11A / PL 30"
                :postinumero      "15141"
                :postitoimipaikka "Lahti"})

(defn- debug-print [info]
  (when config/suomifi-viestit-debug?
    (log/info info)))

(defn- ^:dynamic bytes->base64 [bytes]
  (String. (b64/encode bytes) "UTF-8"))

(defn- ->sanoma [varmenne valvonta-id toimenpide-id osapuoli-id]
  {:tunniste (str "ETP-" valvonta-id "-" toimenpide-id "-" osapuoli-id)
   :versio   "1.0"
   :varmenne varmenne})

(defn- henkilo->asiakas [henkilo]
  {:tunnus (:henkilotunnus henkilo)
   :tyyppi "SSN"
   :osoite {:nimi             (str (:etunimi henkilo) " " (:sukunimi henkilo))
            :lahiosoite       (:jakeluosoite henkilo)
            :postinumero      (:postinumero henkilo)
            :postitoimipaikka (:postitoimipaikka henkilo)
            :maa              (:maa henkilo)}})

(defn- yritys->asiakas [yritys]
  {:tunnus (:ytunnus yritys)
   :tyyppi "CRN"
   :osoite {:nimi             (:nimi yritys)
            :lahiosoite       (:jakeluosoite yritys)
            :postinumero      (:postinumero yritys)
            :postitoimipaikka (:postitoimipaikka yritys)
            :maa              (:maa yritys)}})

(defn- osaapuoli->asiakas [osapuoli]
  (cond
    (kaytto-schema/henkilo? osapuoli) (henkilo->asiakas osapuoli)
    (kaytto-schema/yritys? osapuoli) (yritys->asiakas osapuoli)))

(defn- toimenpide->kohde [type-key]
  (get {:rfi-request {:nimike "Tietopyyntö"
                      :kuvaus "Tämän viestin liitteenä on tietopyyntö"}}
       type-key))

(defn- toimenpide->tiedosto [type-key]
  (get {:rfi-request {:nimi   "tietopyynto.pdf"
                      :kuvaus "Tietopyyntö liitteenä"}}
       type-key))

(defn- add-cover-page [document osapuoli]
  (with-open [baos (ByteArrayOutputStream.)]
    (pdf/merge-pdf
      [(pdf/generate-pdf->input-stream {:layout "pdf/ipost-address-page.html"
                                        :data   {:lahettaja     lahettaja
                                                 :vastaanottaja {:nimi             (str (:etunimi osapuoli) " " (:sukunimi osapuoli))
                                                                 :jakeluosoite     (:jakeluosoite osapuoli)
                                                                 :postinumero      (:postinumero osapuoli)
                                                                 :postitoimipaikka (:postitoimipaikka osapuoli)}}})
       (io/input-stream document)]
      baos)
    (.toByteArray baos)))

(defn- dokumentti->tiedosto [type-key osapuoli dokumentti]
  (let [{:keys [nimi kuvaus]} (toimenpide->tiedosto type-key)
        tiedosto (add-cover-page dokumentti osapuoli)]
    {:nimi    nimi
     :kuvaus  kuvaus
     :sisalto (bytes->base64 tiedosto)
     :muoto   "application/pdf"}))

(defn- ^:dynamic now []
  (Instant/now))

(defn- ->kohde [toimenpide osapuoli tiedosto]
  (let [type-key (toimenpide/type-key (:type-id toimenpide))
        {:keys [nimike kuvaus]} (toimenpide->kohde type-key)]
    {:viranomaistunniste (:diaarinumero toimenpide)
     :nimike             nimike
     :kuvaus-teksti      kuvaus
     :lahetys-pvm        (now)
     :asiakas            (osaapuoli->asiakas osapuoli)
     :tiedostot          (dokumentti->tiedosto type-key osapuoli tiedosto)}))

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

(defn- assert-success [response]
  (when (not= (:tila-koodi response) 202)
    (exception/throw-ex-info! :suomifi-viestit-request-failed (:tila-koodi-kuvaus response))))

(defn- read-response [response]
  (let [coercer (sc/coercer {:tila-koodi        schema/Int
                             :tila-koodi-kuvaus schema/Str
                             :sanoma-tunniste   schema/Str}
                            sc/string-coercion-matcher)]
    (-> response response-parser coercer assert-success)))

(defn- request-handler! [data]
  (let [request-xml (request-create-xml data)
        response (send-request! request-xml)]
    (when response
      (read-response response))))

(defn send-message-to-osapuoli! [toimenpide
                                 osapuoli
                                 dokumentti
                                 & [{:keys [viranomaistunnus palvelutunnus tulostustoimittaja varmenne
                                            yhteyshenkilo-nimi yhteyshenkilo-email
                                            laskutus-tunniste laskutus-salasana
                                            paperitoimitus? laheta-tulostukseen?]
                                     :or   {viranomaistunnus     config/suomifi-viestit-viranomaistunnus
                                            palvelutunnus        config/suomifi-viestit-palvelutunnus
                                            tulostustoimittaja   config/suomifi-viestit-tulostustoimittaja
                                            varmenne             config/suomifi-viestit-varmenne
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
              :sanoma       (->sanoma varmenne (:valvonta-id toimenpide) (:id toimenpide) (:id osapuoli))
              :kysely       (cond-> {:kohteet              (->kohde toimenpide osapuoli dokumentti)
                                     :tulostustoimittaja   tulostustoimittaja
                                     :paperitoimitus?      paperitoimitus?
                                     :laheta-tulostukseen? laheta-tulostukseen?}
                                    (and laskutus-tunniste laskutus-salasana) (assoc :lakutus {:tunniste laskutus-tunniste
                                                                                               :salasana laskutus-salasana}))}]
    (request-handler! data)))