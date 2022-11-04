(ns solita.etp.handler
  (:require [clojure.string :as str]
            [clojure.walk :as w]
            [ring.middleware.cookies :as cookies]
            [ring.util.codec :as codec]
            [reitit.ring :as ring]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.coercion.schema]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.schema :as schema]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.dev :as dev]
            [reitit.spec :as rs]
            [reitit.dev.pretty :as pretty]
            [muuntaja.core :as m]
            [schema.core :as s]
            [solita.etp.api.kayttaja :as kayttaja-api]
            [solita.etp.api.yritys :as yritys-api]
            [solita.etp.api.laatija :as laatija-api]
            [solita.etp.api.geo :as geo-api]
            [solita.etp.api.energiatodistus :as energiatodistus-api]
            [solita.etp.api.valvonta :as valvonta-api]
            [solita.etp.api.valvonta-oikeellisuus :as valvonta-oikeellisuus-api]
            [solita.etp.api.valvonta-kaytto :as valvonta-kaytto-api]
            [solita.etp.api.laskutus :as laskutus-api]
            [solita.etp.api.viesti :as viesti-api]
            [solita.etp.api.sivu :as sivu-api]
            [solita.etp.api.statistics :as statistics-api]
            [solita.etp.config :as config]
            [solita.etp.security :as security]
            [solita.etp.jwt :as jwt]
            [solita.etp.header-middleware :as header-middleware]
            [solita.etp.exception :as exception]
            [solita.common.map :as map]))

(defn tag [tag routes]
  (w/prewalk
   #(if (and (map? %) (contains? % :summary))
      (assoc % :tags #{tag}) %)
   routes))

(defn- req->jwt [request]
  (try
    (jwt/req->verified-jwt-payloads request)
    (catch Throwable _)))

(defn logout-location [req]
  (let [{:keys [data]} (req->jwt req)]
    (if data
      (str config/cognito-logout-url
           "&logout_uri="
           (str (if (:custom:VIRTU_localID data)
                  config/keycloak-virtu-logout-url
                  config/keycloak-suomifi-logout-url)))
      (str config/index-url "/api/logout"))))

(def empty-cookie {:value ""
                   :path "/"
                   :max-age 0
                   :http-only true
                   :secure true})

(def system-routes
  [["/swagger.json"
    {:get {:no-doc true
           :swagger {:info {:title "Energiatodistuspalvelu API"
                            :description ""}}
           :handler (swagger/create-swagger-handler)}}]
   ["/health"
    {:get {:summary "Health check"
           :tags #{"System"}
           :handler (constantly {:status 200})}}]
   ["/login"
    {:get {:summary "Callback used to redirect user back to where they were"
           :tags #{"System"}
           :parameters {:query {:redirect s/Str}}
           :handler (fn [{:keys [parameters]}]
                      (let [redirect (-> parameters :query :redirect)]
                        {:status 302
                         :headers {"Location" (if (str/starts-with?
                                                   redirect
                                                   config/index-url)
                                                redirect
                                                config/index-url)}}))}}]
   ["/logout"
    {:get {:summary    "Callback used to redirect user to cognito logout"
           :tags       #{"System"}
           :middleware [[cookies/wrap-cookies]]
           :handler    (fn [req]
                         {:status  302
                          :headers {"Location" (logout-location req)}
                          :cookies {"AWSELBAuthSessionCookie-0" empty-cookie
                                    "AWSELBAuthSessionCookie-1" empty-cookie}})}}]
   ;; TODO Temporary endpoint for seeing headers added by load balancer
   ["/headers"
    {:get {:summary "Endpoint for seeing request headers"
           :tags #{"System"}
           :handler (fn [{:keys [headers]}]
                      {:status 200
                       :body headers})}}]])

(def routes
  ["/api" {:middleware [[header-middleware/wrap-default-cache]
                        [header-middleware/wrap-default-content-type]]}
   system-routes
   ["/public" {:middleware [[security/wrap-db-application-name]]}
    (concat (tag "Laatijat Public API" laatija-api/public-routes)
            (tag "Geo Public API" geo-api/routes)
            (tag "Energiatodistus Public API"
                 energiatodistus-api/public-routes)
            (tag "Tilastointi Public API"
                 statistics-api/routes))]
   ["/private" {:middleware [[header-middleware/wrap-disable-cache]
                             [security/wrap-jwt-payloads]
                             [security/wrap-whoami-from-jwt-payloads]
                             [security/wrap-access]
                             [security/wrap-db-application-name]]}
    (concat (tag "Käyttäjä API" kayttaja-api/routes)
            (tag "Yritys API" yritys-api/routes)
            (tag "Laatijat Private API" laatija-api/private-routes)
            (tag "Geo Private API" geo-api/routes)
            (tag "Energiatodistus API" energiatodistus-api/private-routes)
            (tag "Oikeellisuuden valvonta API" valvonta-oikeellisuus-api/routes)
            (tag "Käytönvalvonta API" valvonta-kaytto-api/routes)
            (tag "Valvonta API" valvonta-api/routes)
            (tag "Viesti API" viesti-api/routes)
            (tag "Sivu API" sivu-api/routes)
            (tag "Tilastointi API" statistics-api/routes))]
   ["/external" {:middleware [[security/wrap-whoami-from-basic-auth
                               "Access to external API"]
                              [security/wrap-access]
                              [security/wrap-db-application-name]]}
    (concat (tag "Energiatodistus API" energiatodistus-api/external-routes))]
   ["/internal"
    (concat (tag "Laskutus API" laskutus-api/routes)
            (tag "Energiatodistus Internal API" energiatodistus-api/internal-routes)
            (tag "Laatija Internal API" laatija-api/internal-routes))]])

(def route-opts
  {;; Uncomment line below to see diffs of requests in middleware chain
   ;;:reitit.middleware/transform dev/print-request-diffs
   :exception pretty/exception
   :validate rs/validate
   :data {:coercion reitit.coercion.schema/coercion
          :muuntaja m/instance
          :middleware [swagger/swagger-feature
                       parameters/parameters-middleware
                       muuntaja/format-negotiate-middleware
                       muuntaja/format-response-middleware
                       exception/exception-middleware
                       muuntaja/format-request-middleware
                       coercion/coerce-response-middleware
                       coercion/coerce-request-middleware
                       multipart/multipart-middleware]}})

(def router (ring/router routes route-opts))

(def handler
  (ring/ring-handler router
                     (ring/routes
                        (swagger-ui/create-swagger-ui-handler
                          {:path "/api/documentation"
                           :url "/api/swagger.json"
                           :config {:validationUrl nil}
                           :operationsSorter "alpha"})
                        (ring/create-default-handler))))
