(ns solita.common.smtp
  (:require [clojure.tools.logging :as log])
  (:import (javax.mail Message Message$RecipientType Session)
           (javax.mail.internet InternetAddress
                                MimeMessage
                                MimeBodyPart
                                MimeMultipart)))

(def timeout "5000")

(defn mail-properties [port]
  (doto (System/getProperties)
    (.put "mail.transport.protocol" "smtp")
    (.put "mail.smtp.port" port)
    (.put "mail.smtp.starttls.enable" "true")
    (.put "mail.smtp.auth" "true")
    (.put "mail.smtp.connectiontimeout" "5000")
    (.put "mail.smtp.timeout" "5000")))

(defn session [properties]
  (Session/getDefaultInstance properties))

(defn body-mime-body-part [body content-type]
  (doto (MimeBodyPart.)
    (.setContent body content-type)))

(defn attachment-mime-body-part [attachment]
  (doto (MimeBodyPart.)
    (.attachFile attachment)))

(defn multipart [& mime-body-parts]
  (let [mime-multi-part (MimeMultipart.)]
    (doseq [mime-body-part mime-body-parts]
      (.addBodyPart mime-multi-part mime-body-part))
    mime-multi-part))

(defn mime-message [session from-email from-name to subject multipart]
  (doto (MimeMessage. session)
    (.setFrom (InternetAddress. from-email from-name))
    (.setSubject subject)
    (.setRecipients Message$RecipientType/TO (->> to
                                                  (map #(InternetAddress. %))
                                                  into-array))
    (.setContent multipart)))

(defn transport [session]
  (.getTransport session))

(defn send-email! [host port username password from-email from-name to subject
                   body content-type attachments]
  (let [properties (mail-properties port)
        session (session properties)
        body-mime-body-part (body-mime-body-part body content-type)
        attachments-mime-body-part (map attachment-mime-body-part
                                        attachments)
        multipart (apply multipart body-mime-body-part attachments-mime-body-part)
        mime-message (mime-message session
                                   from-email
                                   from-name
                                   to
                                   subject
                                   multipart)
        transport (transport session)]
    (.connect transport host username password)
    (try
      (.sendMessage transport mime-message (.getAllRecipients mime-message))
      (log/info "Email sent " {:to to :subject subject})
      (finally
        (.close transport)))
    nil))
