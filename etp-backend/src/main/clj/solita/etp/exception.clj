(ns solita.etp.exception
  (:require [reitit.ring.middleware.exception :as exception]
            [clojure.tools.logging :as log]
            [solita.common.map :as map]))

(defn throw-ex-info!
  ([map]
    (throw (ex-info (:message map) map)))
  ([type message] (throw-ex-info! (map/bindings->map type message))))

(defn unique-exception-handler [exception request]
  (let [error (ex-data exception)]
    (log/info (str "Unique violation: " (name (:constraint error))
                   " in service: " (:uri request)))
    {:status  409
     :body    error}))

(defn throw-forbidden!
  ([] (throw (ex-info "Forbidden" {:type :forbidden})))
  ([reason] (throw (ex-info "Forbidden" {:type :forbidden :reason reason}))))

(defn forbidden-handler [exception request]
  (let [error (ex-data exception)]
    (log/info (str "Service "
                   (:uri request)
                   " forbidden for access-token: "
                   (get-in request [:headers "x-amzn-oidc-accesstoken"])
                   (or (some->> error :reason (str " - ")) "")))
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
