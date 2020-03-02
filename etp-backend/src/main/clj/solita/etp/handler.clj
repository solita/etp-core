(ns solita.etp.handler
  (:require [clojure.walk :as w]
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
            [solita.etp.api.user :as user-api]
            [solita.etp.api.yritys :as yritys-api]
            [solita.etp.api.laatija :as laatija-api]
            [solita.etp.api.geo :as geo-api]
            [solita.etp.exception :as exception]
            [solita.common.map :as map]))

(defn tag [tag routes]
  (w/prewalk
   #(if (and (map? %) (contains? % :summary))
      (assoc % :tags #{tag}) %)
   routes))

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
                      {:status 302
                       :headers {"Location" (-> parameters :query :redirect)}})}}] ])

(def routes
  ["/api"
   (concat system-routes
           (tag "User API" user-api/routes)
           (tag "Yritys API" yritys-api/routes)
           (tag "Laatijat API" laatija-api/routes)
           (tag "Geo API" geo-api/routes))])

(def route-opts
  {;; Uncomment line below to see diffs of requests in middleware chain
   ;;:reitit.middleware/transform dev/print-request-diffs
   :exception pretty/exception
   :validate rs/validate
   :data {
          :coercion reitit.coercion.schema/coercion
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

