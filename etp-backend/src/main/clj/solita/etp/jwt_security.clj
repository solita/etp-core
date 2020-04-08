(ns solita.etp.jwt-security
  (:require [clojure.tools.logging :as log]
            [buddy.core.keys :as keys]
            [buddy.sign.jwe :as jwe]
            [buddy.sign.jwt :as jwt]
            [clj-http.client :as http]

            [solita.etp.api.response :as response]
            [solita.etp.service.kayttaja-laatija :as kayttaja-laatija-service]
            [solita.etp.service.rooli :as rooli-service]
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
    (jwt/unsign jwt public-key {:alg (-> jwt jwe/decode-header :alg)})
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
        (when (= id (:sub data-payload) (:sub access-payload))
          {:data data-payload
           :access access-payload})))
    (catch Exception e (log/error e (str "Exception when verifying JWTs: "
                                         (.getMessage e))))))

;;
;; Middleware
;;

(defn wrap-jwt-payloads [handler]
  (fn [req]
    (if-let [jwt-payloads (req->verified-jwt-payloads req)]
      (handler (assoc req :jwt-payloads jwt-payloads))
      response/forbidden)))

(defn wrap-whoami [handler]
  (fn [{:keys [db jwt-payloads] :as req}]
    (let [cognitoid (-> jwt-payloads :data :sub)
          email (-> jwt-payloads :data :email)
          whoami (kayttaja-laatija-service/find-whoami db email cognitoid)]
      (if whoami
        (->> (assoc whoami :cognitoid cognitoid :email email)
             (assoc req :whoami)
             handler)
        (do
          (log/error "Unable to find käyttäjä using email in data JWT")
          response/forbidden)))))

(defn wrap-access [handler]
  (fn [{:keys [request-method whoami] :as req}]
    (let [access (-> req :reitit.core/match :data (get request-method) :access)]
      (if (or (nil? access)
              (access whoami))
        (handler req)
        (do
          (log/warn "Current käyttäjä did not satisfy the access predicate for route:"
                    {:method request-method
                     :url (-> req :reitit.core/match :template)
                     :whoami whoami})
          response/forbidden)))))
