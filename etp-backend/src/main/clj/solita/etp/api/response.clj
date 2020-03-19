(ns solita.etp.api.response
  (:require [ring.util.response :as r])
  (:import (clojure.lang ExceptionInfo)))

(defn get-response [body not-found]
  (if (nil? body)
    (r/not-found not-found)
    (r/response body)))

(defn put-response [updated not-found]
  (if (= updated 0)
    (r/not-found not-found)
    (r/response nil)))

(defn- matches-description? [error error-description]
  (let [matcher (dissoc error-description :response)
        matched-error (select-keys error (keys matcher))]
    (= matcher matched-error)))

(defn response-with-exceptions [service-fn error-descriptions]
  (try
    (do
      (r/response (service-fn)))
    (catch ExceptionInfo e
      (let [error (ex-data e)
            description (first (filter (partial matches-description? error)
                                       error-descriptions))]
        (if (nil? description) (throw e)
          {:status  (:response description)
           :headers {}
           :body    error})))))

(defn created [path id]
  (r/created (str path "/" id) {:id id}))

(defn items-created [path ids]
  (r/created (str path "/") {:ids ids}))
