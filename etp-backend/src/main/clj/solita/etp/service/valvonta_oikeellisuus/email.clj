(ns solita.etp.service.valvonta-oikeellisuus.email
  (:require [solita.etp.config :as config]
            [solita.common.smtp :as smtp]
            [clojure.string :as str]
            [solita.etp.service.complete-energiatodistus :as complete-energiatodistus-service]
            [solita.etp.service.kayttaja :as kayttaja-service]
            [solita.etp.service.valvonta-oikeellisuus.toimenpide :as toimenpide]
            [solita.common.map :as map]
            [solita.common.time :as time]
            [solita.common.logic :as logic])
  (:import (java.time LocalDate)))

(defn- send-email! [{:keys [to subject body]}]
  (smtp/send-text-email! config/smtp-host
                         config/smtp-port
                         config/smtp-username
                         config/smtp-password
                         config/email-from-email
                         config/email-from-name
                         [to]
                         subject body "html"))

(def ^:private signature
  "Tämä on energiatodistuspalvelun lähettämä automaattinen viesti. Älä vastaa tähän viestiin.")

(defn- link [title]
  (str "<a href=\"https://{host}/#/valvonta/oikeellisuus/{energiatodistus.versio}/{energiatodistus.id}\">" title "</a>"))

(defn- paragraph [& body] (str "<p>" (str/join " " body) "</p>"))

(defn- html [& body] (str "<html><body>"
                          (str/join "" body)
                          (paragraph signature)
                          "</body></html>"))

(def ^:private address
  (str "{energiatodistus.perustiedot.katuosoite-fi}, "
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
      (link "Katso poikkeamailmoitus energiatodistuspalvelussa."))}

   ;; tietopyynnön toimenpidetyypit
   :rfi-request
   {:subject
    "Tietopyyntö koskien energiatodistusta {energiatodistus.id}"
    :body
    (html
      rfi-order-description
      rfi-order-link
      (paragraph
        "Tietopyyntöön on vastattava {toimenpide.deadline-date} mennessä."))}
   :rfi-order
   {:subject
    "Kehotus vastata tietopyyntöön koskien energiatodistusta {energiatodistus.id}"
    :body
    (html
      (paragraph
        "Kehotamme vastaamaan tietopyyntöön {toimenpide.deadline-date} mennessä.")
      rfi-order-description
      rfi-order-link)}
   :rfi-warning
   {:subject
    "Vastaa tietopyyntöön koskien energiatodistusta {energiatodistus.id}"
    :body
    (html
      (paragraph
        "ARA on lähettänyt teille tästä energiatodistuksesta tietopyynnön ja kehotuksen."
        "ARA antaa varoituksen ja vaatii vastaamaan tietopyyntöön {toimenpide.deadline-date} mennessä.")
      rfi-order-description
      rfi-order-link)}

   ;; Valvontamuistion toimenpidetyypit
   :audit-report
   {:subject
    "Valvontamuistio koskien energiatodistusta {energiatodistus.id}"
    :body
    (html
      (paragraph
        "Sinulle on saapunut valvontamuistio koskien energiatodistusta {energiatodistus.id},"
        address)
      (link "Katso valvontamuistio energiatodistuspalvelussa."))}

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
      (link "Katso valvontamuistio energiatodistuspalvelussa."))}

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
      (link "Katso valvontamuistio energiatodistuspalvelussa."))}})

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

(defn send-toimenpide-email! [db energiatodistus-id toimenpide]
  (logic/if-let*
    [energiatodistus (complete-energiatodistus-service/find-complete-energiatodistus
                       db energiatodistus-id)
     laatija (kayttaja-service/find-kayttaja db (:laatija-id energiatodistus))
     template-type (-> toimenpide :type-id toimenpide/type-key)
     template-values
     {:energiatodistus energiatodistus
      :laatija         laatija
      :toimenpide      toimenpide
      :host            config/service-host}
     template (template-type templates)
     message (map/map-values #(interpolate % template-values) template)]
    (send-email! (-> message
                     (assoc :to (:email laatija))))))

