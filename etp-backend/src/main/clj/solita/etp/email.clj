(ns solita.etp.email
  (:require [solita.common.smtp :as smtp]
            [solita.etp.config :as config]))

(defn send-multipart-email! [{:keys [to subject body subtype reply? attachments]}]
  (smtp/send-multipart-email!
    {:host           config/smtp-host
     :port           config/smtp-port
     :username       config/smtp-username
     :password       config/smtp-password
     :from-email     config/email-from-email
     :from-name      config/email-from-name
     :to             to
     :subject        subject
     :body           body
     :subtype        subtype
     :attachments    attachments
     :reply-to-email (and reply? config/email-reply-to-email)
     :reply-to-name  (and reply? config/email-reply-to-name)}))

(defn send-text-email! [{:keys [to subject body subtype reply?]}]
  (smtp/send-text-email!
    {:host           config/smtp-host
     :port           config/smtp-port
     :username       config/smtp-username
     :password       config/smtp-password
     :from-email     config/email-from-email
     :from-name      config/email-from-name
     :to             to
     :subject        subject
     :body           body
     :subtype        subtype
     :reply-to-email (and reply? config/email-reply-to-email)
     :reply-to-name  (and reply? config/email-reply-to-name)}))
