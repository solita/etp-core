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

(defn public-key-from-jwks [jwks kid]
  (->> jwks :keys (filter #(= (:kid %) kid)) first keys/jwk->public-key))

(defn get-public-key* [trusted-iss kid]
  (let [{:keys [status body]} (-> trusted-iss
                                  trusted-iss->jwks-url
                                  http/get
                                  deref)]
    (when (= status 200)
      (-> body json/read-value (public-key-from-jwks kid)))))

(def get-public-key (memoize get-public-key*))

(defn verified-jwt-payload [jwt public-key]
  (jwt/unsign jwt public-key {:alg (-> jwt jwe/decode-header :alg)}))

(defn middleware-for-alb [handler]
  (fn [{:keys [headers] :as req}]
    (let [{id "x-amzn-oidc-identity"
           jwt "x-amzn-oidc-accesstoken"} headers
          kid (-> jwt jwe/decode-header :kid)
          public-key (get-public-key config/default-trusted-iss kid)
          payload (verified-jwt-payload jwt public-key)]
      (if (= (:sub payload) id)
        payload
        (throw (ex-info "x-amzn-oidc-access-token \"sub\" did not match x-amzn-oidc-identity"))))))
