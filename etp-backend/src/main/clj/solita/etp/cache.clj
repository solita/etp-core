(ns solita.etp.cache
  (:require [clojure.string :as str]))

(def cache-control "Cache-Control")
(def pragma "Pragma")
(def no-store "no-store")
(def no-cache "no-cache")

(defn with-cache-disabled [{:keys [headers] :as resp}]
  (if-not (contains? headers cache-control)
    (-> resp
        (assoc-in [:headers cache-control] no-store)
        (assoc-in [:headers pragma] no-cache))
    resp))

(defn wrap-disable-cache [handler]
  (fn [req]
    (-> req handler with-cache-disabled)))
