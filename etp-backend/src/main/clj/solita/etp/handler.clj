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
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.dev :as dev]
            [reitit.spec :as rs]
            [reitit.dev.pretty :as pretty]
            [muuntaja.core :as m]
            [schema.core :as s]
            [solita.etp.api.user :as user-api]
            [solita.etp.api.yritys :as yritys-api]
            [solita.common.map :as map]))

(defn hello-handler [{:keys [db parameters]}]
  {:status 200
   :body {:msg (str "Hello from " (-> parameters :query :from) "!")
          :db (str db)
          :parameters parameters}})

(defn health-handler [_]
  {:status 200})

(def routes
  ["/api"
   ["/swagger.json"
    {:get {:no-doc true
           :swagger {:info {:title "Energiatodistuspalvelu API"
                            :description ""}}
           :handler (swagger/create-swagger-handler)}}]

   ["/hello"
    {:get {:summary "Responds with message from given recipient. For testing only."
           :parameters {:query {:from s/Str}}
           :tags #{"To be removed"}
           :handler hello-handler}}]
   ["/health"
    {:get {:summary "Health check"
           :tags #{"System"}
           :handler health-handler}}]])

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

(defn tag [tag routes]
  (w/prewalk
    #(if (and (map? %) (contains? % :summary))
       (assoc % :tags #{tag}) %)
    routes))

(def router (ring/router
              (concat routes
                      (tag "User API" user-api/routes)
                      (tag "Yritys API" yritys-api/routes))
              route-opts))

(def handler
  (ring/ring-handler router
                     (ring/routes
                        (swagger-ui/create-swagger-ui-handler
                          {:path "/api/documentation"
                           :url "/api/swagger.json"
                           :config {:validationUrl nil}
                           :operationsSorter "alpha"})
                        (ring/create-default-handler))))

