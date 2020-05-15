(ns solita.etp.exception
  (:require [reitit.ring.middleware.exception :as exception]
            [clojure.tools.logging :as log]))

(defn unique-exception-handler [exception request]
  (let [error (ex-data exception)]
    (log/info (str "Unique violation: " (name (:constraint error))
                   " in service: " (:uri request)))
    {:status  409
     :body    error}))

(defn throw-forbidden! []
  (throw (ex-info "Forbidden" {:type :forbidden})))

(defn forbidden-handler [exception request]
  (let [error (ex-data exception)]
    (log/info (str "Service "
                   (:uri request)
                   " forbidden for access-token "
                   (get-in request [:headers "x-amzn-oidc-accesstoken"])))
    {:status  403
     :body "Forbidden"}))

(defn class-name [object] (.getName (class object)))

(defn default-handler
  "Default safe handler for any exception."
  [^Throwable e request]
  (do
    (log/error e (str "Exception in service: " (:uri request))
               (or (ex-data e) ""))
    {:status 500
     :body {:type (or (:type (ex-data e)) (class-name e))
            :message "Internal system error - see logs for details."}}))

(def exception-middleware
  (exception/create-exception-middleware
    (assoc exception/default-handlers
      ::exception/default default-handler
      :unique-violation unique-exception-handler
      :forbidden forbidden-handler)))
