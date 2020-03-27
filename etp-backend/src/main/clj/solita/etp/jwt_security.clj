(ns solita.etp.jwt-security
  (:require [clojure.tools.logging :as log]
            [buddy.core.keys :as keys]
            [buddy.sign.jwe :as jwe]
            [buddy.sign.jwt :as jwt]
            [clj-http.client :as http]

            ;; TODO json namespace should probably not be
            ;; under service namespace
            [solita.etp.service.json :as json]
            [solita.etp.config :as config]))

(defn http-get [url f]
  (let [{:keys [status body]} (http/get url)]
    (when (= status 200) (f body))))

;;
;; Access token related
;;

(defn public-key-from-jwks [jwks kid]
  (some->> jwks :keys (filter #(= (:kid %) kid)) first keys/jwk->public-key))

(defn get-public-key-for-access-token* [trusted-iss kid]
  (http-get (str trusted-iss ".well-known/jwks.json")
            #(-> % json/read-value (public-key-from-jwks kid))))

(def get-public-key-for-access-token (memoize get-public-key-for-access-token*))

;;
;; Data token related
;;

(defn get-public-key-for-data-token* [base-url kid]
  (http-get (str base-url kid) keys/str->public-key))

(def get-public-key-for-data-token (memoize get-public-key-for-data-token*))

;;
;; Common
;;

(defn verified-jwt-payload [jwt public-key]
  (when (and jwt public-key)
    (jwt/unsign jwt public-key {:alg (-> jwt jwe/decode-header :alg)})))

(defn alb-headers [{:keys [headers]}]
  (let [{id "x-amzn-oidc-identity"
         data "x-amzn-oidc-data"
         access "x-amzn-oidc-accesstoken"} headers]
    (when (and id data access)
      {:id id :data data :access access})))

(def forbidden {:status 403 :body "Forbidden"})

(defn req->jwt-info [req]
  (try
    (do
      (let [{:keys [id data access]} (alb-headers req)
            data-kid (-> data jwe/decode-header :kid)
            data-public-key (get-public-key-for-data-token
                             config/data-jwt-public-key-base-url
                             data-kid)
            {:keys [email] :as data-payload} (verified-jwt-payload
                                              data
                                              data-public-key)
            access-kid (-> access jwe/decode-header :kid)
            access-public-key (get-public-key-for-access-token
                               config/trusted-jwt-iss
                               access-kid)
            access-payload (verified-jwt-payload access access-public-key)]
        (when  (= id (:sub data-payload) (:sub access-payload))
          ;; TODO get user from db, do things
          access-payload)))
    (catch Exception e (log/error e (str "Exception when verifying JWTs: "
                                         (.getMessage e))))))

(defn middleware-for-alb [handler]
  (fn [req]
    (if-let [payload (req->jwt-info req)]
      (handler (assoc req :jwt-payload payload))
      forbidden)))
