(ns solita.etp.service.suomifi-viestit
  (:require [clostache.parser :as clostache]
            [clojure.data.codec.base64 :as b64]
            [solita.etp.schema.valvonta-kaytto :as kaytto-schema]
            [solita.etp.service.valvonta-kaytto.toimenpide :as toimenpide]
            [solita.etp.config :as config]
            [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [solita.etp.exception :as exception])
  (:import (java.time Instant)))

(defn- debug-print [info]
  (when config/suomifi-viestit-debug?
    (log/info info)))

(defn- bytes->base64 [bytes]
  (String. (b64/encode bytes) "UTF-8"))

(defn ->sanoma [valvonta-id toimenpide-id osapuoli-id]
  {:tunniste (str "ETP-" valvonta-id "-" toimenpide-id "-" osapuoli-id)
   :versio   "1.0"
   :varmenne config/suomifi-viestit-varmenne})

(defn- henkilo->asiakas [henkilo]
  {:tunnus (:henkilotunnus henkilo)
   :tyyppi "SSN"
   :osoite {:nimi             (str (:etunimi henkilo) (:sukunimi henkilo))
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

(defn toimenpide->kohde [type-key]
  (get {:rfi-request {:nimike "Päätös ...."
                      :kuvaus "Tämän viestin liitteessä on päätös asiointiasiaan liittyen."}}
       type-key))

(defn toimenpide->tiedosto [type-key]
  (get {:rfi-request {:nimi   "Päätös.pdf"
                      :kuvaus "Liitetiedosto asiaan liittyen"}}
       type-key))

(defn dokumentti->tiedosto [type-key dokumentti]
  (let [{:keys [nimi kuvaus]} (toimenpide->tiedosto type-key)]
    {:nimi    nimi
     :kuvaus  kuvaus
     :sisalto dokumentti
     :muoto   "application/pdf"}))

(defn ->kohde [toimenpide osapuoli]
  (let [type-key (toimenpide/type-key (:type-id toimenpide))
        {:keys [nimike kuvaus]} (toimenpide->kohde type-key)
        dokumentti "tämä on tiedosto"]
    {:viranomaistunniste (:diaarinumero toimenpide)
     :nimike             nimike
     :kuvaus-teksti      kuvaus
     :lahetys-pvm        (Instant/now)
     :asiakas            (osaapuoli->asiakas osapuoli)
     :tiedostot          (dokumentti->tiedosto type-key dokumentti)}))

(defn- request-create-xml [data]
  (let [xml (clostache/render-resource (str "suomifi/viesti.xml") data)]
    (debug-print xml)
    xml))

(defn- ^:dynamic make-send-requst! [request]
  (if config/suomifi-viestit-endpoint-url
    (http/post config/suomifi-viestit-endpoint-url
               {:body request})
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

(defn- request-handler! [data]
  (let [request-xml (request-create-xml data)
        response-xml (send-request! request-xml)]
    response-xml))

(defn send-message-to-osapuoli [toimenpide osapuoli]
  (let [data {:viranomainen {:viranomaistunnus config/suomifi-viestit-viranomaistunnus
                             :palvelutunnus    config/suomifi-viestit-palvelutunnus}
              :sanoma       (->sanoma (:valvonta-id toimenpide) (:id toimenpide) (:id osapuoli))
              :kysely       {:kohteet            (->kohde toimenpide osapuoli)
                             :tulostustoimittaja config/suomifi-viestit-tulostustoimittaja}}]
    (request-handler! data)))