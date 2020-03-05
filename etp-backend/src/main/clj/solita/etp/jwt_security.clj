(ns solita.etp.jwt-security
  (:require [buddy.core.keys :as keys]
            [buddy.sign.jwe :as jwe]
            [buddy.sign.jwt :as jwt]
            [org.httpkit.client :as http]

            ;; TODO json namespace should probably not be
            ;; under service namespace
            [solita.etp.service.json :as json]
            [solita.etp.config :as config]))

(defn trusted-iss->jwks-url [trusted-iss]
  (str trusted-iss "/.well-known/jwks.json"))

(defn get-public-key [url]
  (let [{:keys [status body]} @(http/get url)]
    (when (= status 200)
      (-> body json/read-value keys/jwk->public-key))))

(def get-public-key-by-iss
  (memoize (fn [iss] (-> iss trusted-iss->jwks-url get-public-key))))

(defn verified-jwt-payload [jwt public-key]
  (jwt/unsign jwt public-key {:alg (-> jwt jwe/decode-header :alg)}))

(defn middleware-for-alb [handler]
  (fn [{:keys [headers] :as req}]
    (let [{id "x-amzn-oidc-identity"
           jwt "x-amzn-oidc-accesstoken"} headers
          public-key (get-public-key-by-iss config/default-trusted-iss)
          payload (verified-jwt-payload jwt public-key)]
      (if (= (:sub payload) id)
        payload
        (throw (ex-info "x-amzn-oidc-access-token \"sub\" did not match x-amzn-oidc-identity"))))))
