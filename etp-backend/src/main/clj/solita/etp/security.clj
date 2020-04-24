(ns solita.etp.security
  (:require [clojure.tools.logging :as log]
            [solita.etp.jwt :as jwt]
            [solita.etp.basic-auth :as basic-auth]
            [solita.etp.api.response :as response]
            [solita.etp.service.kayttaja-laatija :as kayttaja-laatija-service]
            [solita.etp.service.rooli :as rooli-service]))

(defn wrap-jwt-payloads [handler]
  (fn [req]
    (if-let [jwt-payloads (jwt/req->verified-jwt-payloads req)]
      (handler (assoc req :jwt-payloads jwt-payloads))
      response/forbidden)))

(defn wrap-whoami-from-jwt-payloads [handler]
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

(defn wrap-whoami-from-basic-auth [handler]
  (fn [{:keys [db] :as req}]
    (let [{:keys [id password]} (basic-auth/req->id-and-password req)
          whoami (kayttaja-laatija-service/find-whoami db id)]
      (if whoami
        (handler (assoc req :whoami whoami))
        (do
          (log/error "Unable to find käyttäjä with Basic Auth")
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
