(ns solita.etp.content-type)

(def content-type "Content-Type")
(def plain-text "text/plain; charset=UTF-8")

(defn with-default-content-type [{:keys [body headers] :as resp}]
  (if (and (string? body) (not (contains? headers content-type)))
    (assoc-in resp [:headers content-type] plain-text)
    resp))

(defn wrap-default-content-type [handler]
  (fn [req]
    (-> req handler with-default-content-type)))
