(ns solita.etp.service.valvonta-oikeellisuus.email
  (:require [solita.etp.config :as config]
            [clojure.string :as str]
            [solita.etp.service.complete-energiatodistus :as complete-energiatodistus-service]
            [solita.etp.service.kayttaja :as kayttaja-service]
            [solita.etp.service.valvonta-oikeellisuus.toimenpide :as toimenpide]
            [solita.common.map :as map]
            [solita.common.time :as time]
            [solita.common.logic :as logic]
            [solita.etp.service.valvonta-oikeellisuus.asha :as asha-valvonta-oikeellisuus]
            [solita.etp.email :as email]
            [solita.common.smtp :as smtp])
  (:import (java.time LocalDate)))



(defn- link [title]
  (str "<a href=\"{host}/#/valvonta/oikeellisuus/{energiatodistus.versio}/{energiatodistus.id}\">" title "</a>"))

(defn- paragraph [& body] (str "<p>" (str/join " " body) "</p>"))

(def ^:private signature-do-not-reply
  (paragraph "Tämä on energiatodistuspalvelun lähettämä automaattinen viesti. Älä vastaa tähän viestiin."))

(def ^:private signature-reply
  (paragraph "Tämä on energiatodistuspalvelun lähettämä automaattinen viesti."))

(defn- html [& body] (str "<html><body>"
                          (str/join "" body)
                          "</body></html>"))

(def ^:private address
  (str "{energiatodistus.perustiedot.nimi}, "
       "{energiatodistus.perustiedot.katuosoite-fi}, "
       "{energiatodistus.perustiedot.postinumero} "
       "{energiatodistus.perustiedot.postitoimipaikka-fi}."))

(def ^:private rfi-order-description
  (paragraph
    "Sinulle on saapunut tietopyyntö koskien energiatodistusta {energiatodistus.id},"
    address))

(def ^:private rfi-order-link
  (link "Katso ja vastaa tietopyyntöön energiatodistuspalvelussa."))

(def ^:private templates
  {;; kevyt valvontamenettely
   :anomaly
   {:subject
    "Poikkeamailmoitus koskien energiatodistusta {energiatodistus.id}"
    :body
    (html
      (paragraph
        "Sinulle on saapunut poikkeamailmoitus koskien energiatodistusta {energiatodistus.id},"
        address)
      (link "Katso poikkeamailmoitus energiatodistuspalvelussa.")
      signature-do-not-reply)}

   ;; tietopyynnön toimenpidetyypit
   :rfi-request
   {:subject
    "Tietopyyntö koskien energiatodistusta {energiatodistus.id}"
    :body
    (html
      rfi-order-description
      rfi-order-link
      (paragraph
        "Tietopyyntöön on vastattava {toimenpide.deadline-date} mennessä.")
      signature-do-not-reply)}
   :rfi-order
   {:subject
    "Kehotus vastata tietopyyntöön koskien energiatodistusta {energiatodistus.id}"
    :body
    (html
      (paragraph
        "Kehotamme vastaamaan tietopyyntöön {toimenpide.deadline-date} mennessä.")
      rfi-order-description
      rfi-order-link
      signature-do-not-reply)}
   :rfi-warning
   {:subject
    "Vastaa tietopyyntöön koskien energiatodistusta {energiatodistus.id}"
    :body
    (html
      (paragraph
        "ARA on lähettänyt teille tästä energiatodistuksesta tietopyynnön ja kehotuksen."
        "ARA antaa varoituksen ja vaatii vastaamaan tietopyyntöön {toimenpide.deadline-date} mennessä.")
      rfi-order-description
      rfi-order-link
      signature-do-not-reply)}

   ;; Valvontamuistion toimenpidetyypit
   :audit-report
   {:subject
    "Valvontamuistio koskien energiatodistusta {energiatodistus.id}"
    :body
    (html
      (paragraph
        "Sinulle on saapunut valvontamuistio koskien energiatodistusta {energiatodistus.id},"
        address)
      (link "Katso valvontamuistio energiatodistuspalvelussa.")
      signature-do-not-reply)}
   :audit-order
   {:subject
    "Kehotus vastata valvontamuistioon koskien energiatodistusta {energiatodistus.id}"
    :body
    (html
      (paragraph
        "Kehotamme vastaamaan valvontamuistioon {toimenpide.deadline-date} mennessä.")
      (paragraph
        "Sinulle on saapunut valvontamuistio koskien energiatodistusta {energiatodistus.id},"
        address)
      (link "Katso valvontamuistio energiatodistuspalvelussa.")
      signature-do-not-reply)}

   :audit-warning
   {:subject
    "Vastaa valvontamuistioon koskien energiatodistusta {energiatodistus.id}"
    :body
    (html
      (paragraph
        "ARA on lähettänyt teille tästä energiatodistuksesta valvontamuistion ja kehotuksen."
        "ARA antaa varoituksen ja vaatii vastaamaan valvontamuistioon {toimenpide.deadline-date} mennessä.")
      (paragraph
        "Sinulle on saapunut valvontamuistio koskien energiatodistusta {energiatodistus.id},"
        address)
      (link "Katso valvontamuistio energiatodistuspalvelussa.")
      signature-do-not-reply)}

   ;; lisäselvityspyyntö
   :rfc-request
   {:subject
    "Lisäselvityspyyntö koskien energiatodistusta {energiatodistus.id}"
    :body
    (html
      (paragraph
        "Sinulle on saapunut lisäselvityspyyntö koskien energiatodistusta {energiatodistus.id},"
        address)
      (link "Katso ja vastaa lisäselvityspyyntöön energiatodistuspalvelussa.")
      (paragraph
        "Lisäselvityspyyntöön on vastattava {toimenpide.deadline-date} mennessä.")
      signature-do-not-reply)}})

(def ^:private tiedoksi-template
  {:subject
   "Valvontamuistio tiedoksi koskien energiatodistusta {energiatodistus.id}"
   :body
   (html
     (paragraph
       "Ohessa valvontamuistio tiedoksi koskien energiatodistusta {energiatodistus.id},"
       address)
     signature-reply)})

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

(defn- send-email-to-laatija! [db energiatodistus-id toimenpide]
  (logic/if-let*
    [energiatodistus (complete-energiatodistus-service/find-complete-energiatodistus
                       db energiatodistus-id)
     laatija (kayttaja-service/find-kayttaja db (:laatija-id energiatodistus))
     template-type (-> toimenpide :type-id toimenpide/type-key)
     template-values
     {:energiatodistus energiatodistus
      :laatija         laatija
      :toimenpide      toimenpide
      :host            config/index-url}
     template (template-type templates)
     message (map/map-values #(interpolate % template-values) template)]
    (email/send-text-email! (assoc message :to [(:email laatija)]
                                           :subtype "html"))))

(defn- send-email-to-tiedoksi! [db aws-s3-client energiatodistus-id toimenpide-id email]
  (logic/if-let*
    [energiatodistus (complete-energiatodistus-service/find-complete-energiatodistus
                       db energiatodistus-id)
     template-values {:energiatodistus energiatodistus
                      :host            config/index-url}
     message (map/map-values #(interpolate % template-values) tiedoksi-template)
     valvontamuistio (asha-valvonta-oikeellisuus/find-document aws-s3-client energiatodistus-id toimenpide-id)]
    (email/send-multipart-email! (assoc message :to [email]
                                                :subtype "html"
                                                :reply? true
                                                :attachments [(smtp/input-stream->attachment
                                                                valvontamuistio
                                                                "valvontamuistio.pdf"
                                                                "application/pdf")]))))

(defn send-toimenpide-email! [db aws-s3-client energiatodistus-id toimenpide]
  (send-email-to-laatija! db energiatodistus-id toimenpide)
  (when-let [tiedoksi (and (toimenpide/audit-report? toimenpide)
                           (seq (filter #(-> % :email seq) (:tiedoksi toimenpide))))]
    (doseq [vastaanottaja tiedoksi]
      (send-email-to-tiedoksi! db aws-s3-client energiatodistus-id (:id toimenpide) (:email vastaanottaja)))))