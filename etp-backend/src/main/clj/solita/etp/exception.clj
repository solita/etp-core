(ns solita.etp.exception
  (:require [reitit.ring.middleware.exception :as exception]
            [clojure.tools.logging :as log]))

(defn unique-exception-handler [exception request]
  (let [error (ex-data exception)]
    (log/info (str "Unique violation: " (name (:constraint error))
                   " in service: " (:uri request)))
    {:status  409
     :body    error}))

(defn default-handler
  "Default safe handler for any exception."
  [^Throwable e request]
  (do
    (log/error e (str "Exception in service: " (:uri request)))
    {:status 500
     :body {:type "exception"
            :class (.getName (.getClass e))}}))

(def exception-middleware
  (exception/create-exception-middleware
    (assoc exception/default-handlers
      ::exception/default default-handler
      :unique-violation unique-exception-handler)))

