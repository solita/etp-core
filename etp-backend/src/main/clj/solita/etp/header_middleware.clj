(ns solita.etp.header-middleware
  (:require [clojure.string :as str]))

(def content-type "Content-Type")
(def text-plain "text/plain; charset=UTF-8")

(def cache-control "Cache-Control")
(def pragma "Pragma")
(def no-store "no-store")
(def no-cache "no-cache")

(defn contains-header? [{:keys [headers]} header]
  (->> headers
       (filter (fn [[header-k]] (= (str/lower-case header-k)
                                  (str/lower-case header))))
       empty?
       not))

(defn with-default-header [{:keys [headers] :as resp} header-k header-v]
  (cond-> resp
    (not (contains-header? resp header-k))
    (assoc-in [:headers header-k] header-v)))

(defn wrap-default-content-type [handler]
  (fn [req]
    (let [resp (handler req)]
      (cond-> resp
        (-> resp :body string?)
        (with-default-header content-type text-plain)))))

(defn wrap-disable-cache [handler]
  (fn [req]
    (let [resp (handler req)]
      (cond-> resp
        (not (contains-header? resp cache-control))
        (-> (with-default-header cache-control no-store)
            (with-default-header pragma no-cache))))))

(defn wrap-cache-control [handler seconds]
  (fn [req]
    (let [resp (handler req)]
      (cond-> resp
        (not (contains-header? resp cache-control))
        (-> (with-default-header cache-control (str "max-age=" seconds ",public"))
            (with-default-header pragma nil))))))
