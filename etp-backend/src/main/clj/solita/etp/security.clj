(ns solita.etp.security
  (:require [clojure.tools.logging :as log]
            [solita.common.jdbc :as common-jdbc]
            [solita.etp.jwt :as jwt]
            [solita.etp.basic-auth :as basic-auth]
            [solita.etp.api.response :as response]
            [solita.etp.service.whoami :as whoami-service]
            [solita.etp.service.rooli :as rooli-service]))

(defn wrap-jwt-payloads [handler]
  (fn [req]
    (if-let [jwt-payloads (jwt/req->verified-jwt-payloads req)]
      (handler (assoc req :jwt-payloads jwt-payloads))
      response/forbidden)))

(defn wrap-whoami-from-jwt-payloads [handler]
  (fn [{:keys [db jwt-payloads] :as req}]
    (let [{:keys [data]} jwt-payloads
          email (:email data)
          cognitoid (:sub data)
          whoami (whoami-service/find-whoami
                  db
                  {:email email
                   :cognitoid cognitoid
                   :henkilotunnus (:custom:FI_nationalIN data)
                   :virtulocalid (:custom:VIRTU_localID data)
                   :virtuorganisaatio (:custom:VIRTU_localOrg data)})]
      (if whoami
        (->> (cond-> whoami
               email (assoc :email email)
               cognitoid (assoc :cognitoid cognitoid))
             (assoc req :whoami)
             handler)
        (do
          (log/error "Unable to find käyttäjä using email in data JWT")
          response/forbidden)))))

(defn wrap-whoami-from-basic-auth [handler]
  (fn [{:keys [db] :as req}]
    (let [{:keys [id password]} (basic-auth/req->id-and-password req)
          whoami (whoami-service/find-whoami db {:email id})]
      (if whoami
        (handler (assoc req :whoami whoami))
        (do
          (log/error "Unable to find käyttäjä with Basic Auth"
                     {:id id})
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

(defn wrap-db-application-name [handler]
  (fn [{:keys [whoami] :as req}]
    (let [application-name (str (:id whoami) "@core.etp")]
    (common-jdbc/with-application-name-support
      #(handler (assoc-in
                  req [:db :application-name]
                  application-name))))))
