(ns solita.etp.service.valvonta-kaytto.suomifi-viestit
  (:require [clostache.parser :as clostache]
            [clojure.data.codec.base64 :as b64]
            [solita.etp.schema.valvonta-kaytto :as kaytto-schema]
            [solita.etp.service.valvonta-kaytto.toimenpide :as toimenpide]
            [solita.etp.service.suomifi-viestit :as suomifi]

            [clojure.java.io :as io]
            [solita.etp.service.pdf :as pdf]

            [solita.common.time :as time]
            [solita.etp.service.valvonta-kaytto.store :as store])
  (:import (java.time Instant)
           (java.io ByteArrayOutputStream File)))

(def lahettaja {:nimi             "Asumisen rahoitus- ja kehittämiskeskus"
                :jakeluosoite     "Vesijärvenkatu 11A / PL 30"
                :postinumero      "15141"
                :postitoimipaikka "Lahti"})

(defn- ->sanoma [valvonta-id toimenpide-id osapuoli-id]
  {:tunniste (str "ETP-" valvonta-id "-" toimenpide-id "-" osapuoli-id)
   :versio   "1.0"})

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

(defn kuvaus [type-key valvonta toimenpide]
  (clostache/render (str "Tämän viestin liitteenä on tietopyyntö koskien rakennustasi: {{rakennustunnus}}\n"
                         "{{katuosoite}}, {{postinumero}} {{postitoimipaikka-fi}}\n"
                         "{{#rfi-request}}Tietopyyntöön on vastattava {{deadline-date}} mennessä.{{/rfi-request}}"
                         "{{#rfi-order}}Kehotamme vastaamaan tietopyyntöön {{deadline-date}} mennessä.{{/rfi-order}}"
                         "{{#rfi-warning}}ARA on lähettänyt teille tietopyynnön ja kehotuksen."
                         "ARA antaa varoituksen ja vaatii vastaamaan tietopyyntöön {{deadline-date}} mennessä.{{/rfi-warning}}")
                    {:rakennustunnus      (:rakennustunnus valvonta)
                     :katuosoite          (:katuosoite valvonta)
                     :postinumero         (:postinumero valvonta)
                     :postitoimipaikka-fi (:postitoimipaikka-fi valvonta)
                     :deadline-date       (time/format-date (:deadline-date toimenpide))
                     type-key             true}))

(defn- toimenpide->kohde [type-key valvonta toimenpide]
  (get {:rfi-request {:nimike "Tietopyyntö"
                      :kuvaus (kuvaus :rfi-request valvonta toimenpide)}
        :rfi-order   {:nimike "Kehotus vastata tietopyyntöön"
                      :kuvaus (kuvaus :rfi-order valvonta toimenpide)}
        :rfi-warning {:nimike "Vastaa tietopyyntöön"
                      :kuvaus (kuvaus :rfi-warning valvonta toimenpide)}}
       type-key))

(defn- toimenpide->tiedosto [type-key]
  (get {:rfi-request {:nimi   "tietopyynto.pdf"
                      :kuvaus "Tietopyyntö liitteenä"}
        :rfi-order   {:nimi   "tietopyynto_kehtotus.pdf"
                      :kuvaus "Tietopyynnön kehotus liitteenä"}
        :rfi-warning {:nimi   "tietopyynto_varoitus.pdf"
                      :kuvaus "Tietopyynnön varoitus liitteenä"}}
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

(defn- ^:dynamic bytes->base64 [bytes]
  (String. (b64/encode bytes) "UTF-8"))

(defn- dokumentti->tiedosto [type-key osapuoli dokumentti]
  (let [{:keys [nimi kuvaus]} (toimenpide->tiedosto type-key)
        tiedosto (add-cover-page dokumentti osapuoli)]
    {:nimi    nimi
     :kuvaus  kuvaus
     :sisalto (bytes->base64 tiedosto)
     :muoto   "application/pdf"}))

(defn- ^:dynamic now []
  (Instant/now))

(defn- ->kohde [valvonta toimenpide osapuoli tiedosto]
  (let [type-key (toimenpide/type-key (:type-id toimenpide))
        {:keys [nimike kuvaus]} (toimenpide->kohde type-key valvonta toimenpide)]
    {:viranomaistunniste (:diaarinumero toimenpide)
     :nimike             nimike
     :kuvaus-teksti      kuvaus
     :lahetys-pvm        (now)
     :asiakas            (osaapuoli->asiakas osapuoli)
     :tiedostot          (dokumentti->tiedosto type-key osapuoli tiedosto)}))

(defn send-message-to-osapuoli! [valvonta
                                 toimenpide
                                 osapuoli
                                 dokumentti
                                 & [config]]
  (suomifi/send-message!
    (->sanoma (:id valvonta) (:id toimenpide) (:id osapuoli))
    (->kohde valvonta toimenpide osapuoli dokumentti)
    config))

(defn send-suomifi-viestit! [aws-s3-client
                             valvonta
                             toimenpide
                             osapuolet]
  (doseq [osapuoli (->> osapuolet
                        kaytto-schema/omistaja?
                        kaytto-schema/toimitustapa-suomifi?)]
    (send-message-to-osapuoli!
      valvonta
      toimenpide
      osapuoli
      (store/find-document aws-s3-client (:valvonta-id toimenpide) (:id toimenpide) osapuoli))))