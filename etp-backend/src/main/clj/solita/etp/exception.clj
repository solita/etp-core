(ns solita.etp.exception
  (:require [reitit.ring.middleware.exception :as exception]
            [ring.util.response :as r]))

(defn unique-exception-handler [exception _]
  (let [error (ex-data exception)]
    {:status  409
     :body    error}))

(def exception-middleware
  (exception/create-exception-middleware
    (assoc exception/default-handlers
      :unique-violation unique-exception-handler)))

