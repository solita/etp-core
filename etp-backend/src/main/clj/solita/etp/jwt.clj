(ns solita.etp.jwt
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [buddy.core.keys :as keys]
            [buddy.sign.jwe :as jwe]
            [buddy.sign.jwt :as jwt]
            [buddy.core.codecs :as codecs]
            [buddy.core.codecs.base64 :as b64]
            [clj-http.client :as http]

            ;; TODO json namespace should probably not be
            ;; under service namespace
            [solita.etp.service.json :as json]
            [solita.etp.config :as config]))

(defn http-get [url f]
  (let [{:keys [status body] :as resp} (http/get url {:throw-exceptions false})]
    (if (= status 200)
      (f body)
      (log/error "Fail in HTTP GET: " {:url url
                                       :response resp}))))

;;
;; Access token related
;;

(defn public-key-from-jwks [jwks kid]
  (some->> jwks :keys (filter #(= (:kid %) kid)) first keys/jwk->public-key))

(defn get-public-key-for-access-token* [trusted-iss kid]
  (http-get (str trusted-iss "/.well-known/jwks.json")
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
  (if (and jwt public-key)
    (jwt/unsign jwt public-key {:alg (-> jwt jwe/decode-header :alg)
                                :leeway 15})
    (log/warn "JWT verification was not possible due to missing token or public key")))

(defn alb-headers [{:keys [headers]}]
  (let [{id "x-amzn-oidc-identity"
         data "x-amzn-oidc-data"
         access "x-amzn-oidc-accesstoken"} headers]
    (if (and id data access)
      {:id id :data data :access access}
      (log/warn "Missing at least one of the required authentication headers."))))

(defn req->verified-jwt-payloads [req]
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
        (if (= id (:sub data-payload) (:sub access-payload))
          {:data data-payload
           :access access-payload}
          (log/warn "Identity header and data and access token subs did not match."
                    {:id id
                     :access access-payload
                     :data data-payload}))))
    (catch Exception e (log/error e (str "Exception when verifying JWTs: "
                                         (.getMessage e))))))
