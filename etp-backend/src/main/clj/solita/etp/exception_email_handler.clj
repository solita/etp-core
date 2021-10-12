(ns solita.etp.exception-email-handler
  (:require [commonmark-hiccup.core :as ch]
            [solita.etp.config :as config])
  (:import (org.commonmark.node SoftLineBreak)))

(defn- html [& body] (str "<html><body>"
                          (str/join "" body)
                          "</body></html>"))

(def ^:private template
  {:suomifi-viestit-attribute-exception  {:subject "Suomifi viestin lähettäminen epäonnistui: {{sanoma-tunniste}}"
                                          :body    (str "Viestin lähettäminen epäonnistui\n"
                                                        "- Sanoman tunniste: {{sanoma-tunniste}} \n"
                                                        "- Tila: {{tila-koodi}} \n"
                                                        "- Tilan-kuvaus: {{tila-koodi-kuvaus}}")}
   :suomifi-viestit-connection-exception {:subject "Yhteys suomifi viestit palveluun epäonnistui: {{sanoma-tunniste}}"
                                          :body    (str "Yhteys suomifi viestit palveluun epäonnistui\n"
                                                        "- Sanoman tunniste: {{sanoma-tunniste}}")}})

(defn- prepare-email [{:keys [subject body]} data]
  (let [config (update-in ch/default-config
                          [:renderer :nodes SoftLineBreak] (constantly [:br]))]
    {:subject (clostache/render subject data)
     :body    (->> (clostache/render body data) (ch/markdown->html config) html)
     :subtype "html"
     :to      config/email-exception-info}))

(defn exception-handler [^Throwable t error-description]
  (let [{:keys [type data reason]} (-> t ex-data)]
    (if (type template)
      (do
        (email/send-text-email! (prepare-email (type template) data))
        (log/error t reason))
      (log/error t error-description))))