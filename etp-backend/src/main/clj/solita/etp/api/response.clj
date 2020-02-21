(ns solita.etp.api.response
  (:require [ring.util.response :as r]))

(defn get-response [body not-found]
  (if (nil? body)
    (r/not-found not-found)
    (r/response body)))

(defn put-response [updated not-found]
  (if (= updated 0)
    (r/not-found not-found)
    (r/response nil)))

(defn created [path id]
  (r/created (str path "/" id) {:id id}))
