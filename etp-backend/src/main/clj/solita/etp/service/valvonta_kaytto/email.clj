(ns solita.etp.service.valvonta-kaytto.email
  (:require [clojure.string :as str]
            [solita.etp.service.valvonta-kaytto.toimenpide :as toimenpide]
            [solita.common.map :as map]
            [solita.common.time :as time]
            [solita.common.logic :as logic]
            [solita.etp.email :as email]
            [solita.etp.service.valvonta-kaytto.osapuoli :as osapuoli]
            [solita.etp.service.geo :as geo-service]
            [solita.etp.service.luokittelu :as luokittelu-service]
            [solita.common.maybe :as maybe]
            [solita.etp.service.valvonta-kaytto.store :as store]
            [solita.etp.service.pdf :as pdf]
            [clojure.java.io :as io])
  (:import (java.time LocalDate)))

(defn- paragraph [& body] (str "<p>" (str/join " " body) "</p>"))

(def ^:private signature-reply
  (paragraph "Tämä on energiatodistuspalvelun lähettämä automaattinen viesti."))

(defn- html [& body] (str "<html><body>"
                          (str/join "" body)
                          "</body></html>"))

(def ^:private address
  (str "{valvonta.katuosoite}, "
       "{valvonta.postinumero} "
       "{valvonta.postitoimipaikka-fi}."))

(def ^:private rfi-order-description
  (paragraph
    "Tämän sähköpostin liitteenä on tietopyyntö koskien rakennustasi: {valvonta.rakennustunnus}"
    address))

(def ^:private templates-omistaja
  {:rfi-request
   {:subject
    "Tietopyyntö"
    :body
    (html
      rfi-order-description
      (paragraph
        "Tietopyyntöön on vastattava {toimenpide.deadline-date} mennessä.")
      signature-reply)}
   :rfi-order
   {:subject
    "Kehotus vastata tietopyyntöön"
    :body
    (html
      (paragraph
        "Kehotamme vastaamaan tietopyyntöön {toimenpide.deadline-date} mennessä.")
      rfi-order-description
      signature-reply)}
   :rfi-warning
   {:subject
    "Vastaa tietopyyntöön"
    :body
    (html
      (paragraph
        "ARA on lähettänyt teille tietopyynnön ja kehotuksen."
        "ARA antaa varoituksen ja vaatii vastaamaan tietopyyntöön {toimenpide.deadline-date} mennessä.")
      rfi-order-description
      signature-reply)}})

(def ^:private templates-tiedoksi
  {:rfi-request
   {:subject
    "Tietopyyntö (tiedoksi)"
    :body
    (html
      (paragraph
        "Sähköpostin liitteenä on tiedoksi tietopyyntö rakennuksesta: {valvonta.rakennustunnus}"
        address)
      (paragraph "Tietopyyntöön on vastattava {toimenpide.deadline-date} mennessä.")
      signature-reply)}})

(defprotocol TemplateValue (view [value]))

(extend-protocol TemplateValue
  Object
  (view [value] (str value))

  LocalDate
  (view [date] (time/format-date date))

  nil
  (view [_] ""))

(defn interpolate [template values]
  (str/replace template #"\{(.*?)\}"
               #(view (get-in values (map keyword (-> % second (str/split #"\.")))))))

(defn- send-email! [valvonta toimenpide osapuoli documents templates]
  (logic/if-let*
    [template-type (-> toimenpide :type-id toimenpide/type-key)
     template-values
     {:valvonta    valvonta
      :toimenpide  toimenpide
      :osapuoli    osapuoli}
     template (template-type templates)
     message (map/map-values #(interpolate % template-values) template)]
    (email/send-multipart-email! (assoc message :to [(:email osapuoli)]
                                                :subtype "html"
                                                :reply? true
                                                :attachments documents))))

(defn- find-document [aws-s3-client valvonta toimenpide osapuoli]
  (when-let [document (store/find-document aws-s3-client (:id valvonta) (:id toimenpide) osapuoli)]
    (io/input-stream (pdf/merge-pdf [document (store/info-letter)]))))

(defn- send-email-to-omistaja! [aws-s3-client valvonta toimenpide osapuoli]
  (when-let [document (find-document aws-s3-client valvonta toimenpide osapuoli)]
    (send-email! valvonta toimenpide osapuoli [document] templates-omistaja)))

(defn send-toimenpide-email! [db aws-s3-client valvonta toimenpide osapuolet]
  (let [postinumero (maybe/map* #(luokittelu-service/find-luokka
                                   (Integer/parseInt %)
                                   (geo-service/find-all-postinumerot db))
                                (:postinumero valvonta))
        valvonta (assoc valvonta
                   :postitoimipaikka-fi (:label-fi postinumero)
                   :postitoimipaikka-sv (:label-sv postinumero))
        email-osapuolet (filter osapuoli/email? osapuolet)
        documents (mapv (partial store/find-document aws-s3-client (:id valvonta) (:id toimenpide))
                        (filter osapuoli/omistaja? osapuolet))]
    (doseq [vastaanottaja (filter osapuoli/omistaja? email-osapuolet)]
      (send-email-to-omistaja! aws-s3-client valvonta toimenpide vastaanottaja))
    (when-not (empty? documents)
      (doseq [vastaanottaja (filter osapuoli/tiedoksi? email-osapuolet)]
        (send-email! valvonta toimenpide vastaanottaja documents templates-tiedoksi)))))