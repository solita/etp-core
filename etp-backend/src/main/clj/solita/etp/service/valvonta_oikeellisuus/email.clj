(ns solita.etp.service.valvonta-oikeellisuus.email
  (:require [solita.etp.config :as config]
            [solita.common.smtp :as smtp]
            [clojure.string :as str]
            [solita.etp.service.complete-energiatodistus :as complete-energiatodistus-service]
            [solita.etp.service.kayttaja :as kayttaja-service]
            [solita.etp.service.valvonta-oikeellisuus.toimenpide :as toimenpide]
            [solita.common.map :as map]
            [solita.common.time :as time])
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
  (str "<a href=\"https://{host}/#/valvonta/oikeellisuus/{id}\">" title "</a>"))

(defn- paragraph [& body] (str "<p>" (str/join " " body) "</p>"))

(def ^:private templates
  {;; kevyt valvontamenettely
   :anomaly
   {:subject "Poikkeamailmoitus koskien energiatodistusta {energiatodistus.id}"
    :body ""}

   ;; tietopyynnön toimenpidetyypit
   :rfi-request
   {:subject "Tietopyyntö koskien energiatodistusta {energiatodistus.id}"
    :body
    (str (paragraph
           "Sinulle on saapunut tietopyyntö koskien energiatodistusta {energiatodistus.id},"
           "{energiatodistus.perustiedot.katuosoite-fi},"
           "{energiatodistus.perustiedot.postinumero}"
           "{energiatodistus.perustiedot.postitoimipaikka-fi}.")
         (link "Katso ja vastaa tietopyyntöön energiatodistuspalvelussa.")
         (paragraph
          "Tietopyyntöön on vastattava {toimenpide.deadline-date} mennessä."))}
   :rfi-order {}
   :rfi-warning {}

   ;; valvonnan toimenpidetyypit
   :audit-report {:subject "test" :body "test"}
   :audit-order {:subject "test" :body "test"}
   :audit-warning {:subject "test" :body "test"}

   ;; lisäselvityspyynnön toimenpidetyypit
   :rfc-request {:subject "test" :body "test"}})

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
  (let [energiatodistus (complete-energiatodistus-service/find-complete-energiatodistus
                          db energiatodistus-id)
        laatija (kayttaja-service/find-kayttaja db (:laatija-id energiatodistus))
        toimenpide-type (-> toimenpide :type-id toimenpide/type-key)
        template-values
        {:energiatodistus energiatodistus
         :laatija laatija
         :toimenpide toimenpide
         :host config/service-host}
        template (toimenpide-type templates)
        message (map/map-values #(interpolate % template-values) template)]
    (send-email! (-> message
                     (assoc :to (:email laatija))
                     (update :body #(str % (paragraph signature)))))))

