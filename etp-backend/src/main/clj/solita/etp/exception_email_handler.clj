(ns solita.etp.exception-email-handler
  (:require [commonmark-hiccup.core :as ch]
            [solita.etp.config :as config]
            [clojure.tools.logging :as log]
            [solita.etp.email :as email]
            [clojure.string :as str]
            [clostache.parser :as clostache])
  (:import (org.commonmark.node SoftLineBreak)))

(defn- html [& body] (str "<html><body>"
                          (str/join "" body)
                          "</body></html>"))

(def ^:private template
  {:suomifi-viestit-attribute-exception  {:subject "Suomifi viestin lähettäminen epäonnistui: {{sanoma-tunniste}}"
                                          :body    (str "Suomifi viestin lähettäminen epäonnistui\n"
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

(defn exception-handler [^Throwable t]
  (let [{:keys [type data]} (-> t ex-data)]
    (when (type template)
      (try
        (email/send-text-email! (prepare-email (type template) data))
        (catch Throwable th
          (log/error th "Failed to send error email"))))))