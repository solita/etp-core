(ns solita.etp.api.response
  (:require [ring.util.response :as r])
  (:import (clojure.lang ExceptionInfo)))

(defn get-response [body not-found]
  (if (nil? body)
    (r/not-found not-found)
    (r/response body)))

(defn put-response [updated not-found]
  (if (or (nil? updated) (= updated 0))
    (r/not-found not-found)
    (r/response nil)))

(defn- matches-description? [error error-description]
  (let [matcher (dissoc error-description :response)
        matched-error (select-keys error (keys matcher))]
    (= matcher matched-error)))

(defn response-with-exceptions
  ([service-fn error-descriptions] (response-with-exceptions 200 service-fn error-descriptions))
  ([status service-fn error-descriptions]
    (try
      {:status  status
       :headers {}
       :body    (service-fn)}
      (catch ExceptionInfo e
        (let [error (ex-data e)
              description (first (filter (partial matches-description? error)
                                         error-descriptions))]
          (if (nil? description) (throw e)
            {:status  (:response description)
             :headers {}
             :body    error}))))))

(defn created [path id]
  (r/created (str path "/" id) {:id id}))

(def forbidden {:status 403 :body "Forbidden"})

(defn pdf-response [body filename not-found]
  (if (nil? body)
    (r/not-found not-found)
    {:status 200
     :headers {"Content-Type" "application/pdf"
               "Content-Disposition:" (str "inline; filename=\"" filename"\"")}
     :body body}))

(defn conflict [body]
  {:status 409
   :body body})

(defn signature-response [result entity-name]
  (cond
    (= result :already-signed)
    (conflict (str entity-name " already signed"))

    (= result :already-in-signing)
    (conflict (str entity-name " is already in signing process"))

    (= result :not-in-signing)
    (conflict (str "Signing process for " entity-name " has not been started"))

    (= result :pdf-exists)
    (conflict (str "Signed PDF exists for " entity-name ". Get digest to sign again."))

    (nil? result)
    (r/not-found (str entity-name " does not exist"))

    (= result :ok)
    (r/response "Ok")

    :else
    (r/response result)))
