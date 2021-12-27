(ns solita.etp.service.laatija.email
  (:require [solita.etp.email :as email]
            [solita.etp.service.concurrent :as concurrent]
            [solita.common.logic :as logic]))

(def ^:private signature
  (str
    (email/paragraph
      "Tämä on energiatodistuspalvelun lähettämä automaattinen viesti."
      "Älä vastaa tähän viestiin.")
    (email/paragraph "Asumisen rahoitus- ja kehittämiskeskus")))

(def ^:private general-info
  (email/paragraph
    "Energiatodistusrekisterin osoite on <a href=\"https://www.energiatodistusrekisteri.fi\">www.energiatodistusrekisteri.fi</a>."
    "Energiatodistusrekisteriin kirjaudutaan Suomi.fi-tunnistautumisen kautta."
    "Energiatodistusrekisterin käyttöohjeet löytyvät energiatodistusrekisterin kohdasta <em>Laatijan ohjeet</em>."))

(def messages
  {:new
   {:reply? false :subtype "html"
    :subject
    "Tervetuloa energiatodistusrekisterin uudeksi laatijaksi"
    :body
    (email/html
      (email/paragraph "Hei!")
      (email/paragraph
      "Sinut on lisätty energiatodistusrekisteriin."
      "Tarkemmat tiedot löytyvät energiatodistusrekisterissä.")
      general-info
      signature)}

   :patevyystaso
   {:reply? false :subtype "html"
    :subject
    "Energiatodistuksen laatijan pätevyystason muutos"
    :body
    (email/html
      (email/paragraph "Hei!")
      (email/paragraph
        "Sinun pätevyystasoa on muutettu energiatodistusrekisterissä.")
      general-info
      signature)}

   :toteamispaivamaara
   {:reply? false :subtype "html"
    :subject
    "Energiatodistuksen laatijan pätevyyden uusiminen"
    :body
    (email/html
      (email/paragraph "Hei!")
      (email/paragraph
        "Sinun pätevyytesi on uusittu energiatodistusrekisterissä.")
      general-info
      signature)}})

(defn send-email! [recipient]
  (logic/if-let*
    [message (some-> recipient :type messages)
     email (:email recipient)]
    (->
      #(email/send-text-email! (assoc message :to [email]))
      (concurrent/retry 3 1000 Throwable)
      (concurrent/safe (str "Sending email failed for laatija: " (:id recipient) " / " email))
      concurrent/call!)))

(defn send-emails! [recipients]
  (concurrent/run-background
    #(doseq [recipient recipients] (send-email! recipient))
    "Sending laatija emails failed."))