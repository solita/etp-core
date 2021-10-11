(ns solita.etp.service.concurrent
  (:require [clojure.tools.logging :as log]
            [commonmark-hiccup.core :as ch]
            [clostache.parser :as clostache]
            [clojure.string :as str]
            [solita.etp.email :as email]
            [solita.etp.config :as config]))

(defn run-background
  "Executes the given function asynchronously as a background service.
  Returns immediately nil and exceptions are only logged.
  Execution is implemented using clojure future."
  [fn error-description]
  (future
    (try
      (fn)
      (catch Throwable t
        (log/error t error-description))))
  nil)

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

(defn prepare-email [{:keys [subject body]} data]
  (let [config (update-in ch/default-config
                          [:renderer :nodes org.commonmark.node.SoftLineBreak] (constantly [:br]))]
    {:subject (clostache/render subject data)
     :body    (->> (clostache/render body data) (ch/markdown->html config) html)
     :subtype "html"
     :to      config/email-exception-info}))

(defn run-background-errors-to-email
  "Executes the given function asynchronously as a background service.
   Returns immediately nil and exceptions are emailed and logged.
   Execution is implemented using clojure future. "
  [fn]
  (future
    (try
      (fn)
      (catch Throwable t
        (let [{:keys [type data reason]} (-> t ex-data)
              message (prepare-email (type template) data)]
          (log/error t reason)
          (email/send-text-email! message))))
    nil))