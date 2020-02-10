(ns solita.etp.handler
  (:require [reitit.ring :as ring]
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
            [reitit.dev.pretty :as pretty]
            [muuntaja.core :as m]
            [schema.core :as s]
            [solita.etp.api.user :as user-api]
            [solita.common.map :as map]))

(defn hello-handler [{:keys [ctx parameters]}]
  {:status 200
   :body {:msg (str "Hello from " (-> parameters :query :from) "!")
          :ctx (str ctx)
          :parameters parameters}})

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
           :tags #{"Testing"}
           :handler hello-handler}}]])

(def route-opts
  {;; Uncomment line below to see diffs of requests in middleware chain
   ;;:reitit.middleware/transform dev/print-request-diffs
   :exception pretty/exception
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

(defn assoc-tag-for-route [tag route]
  (update route 1 (partial map/map-values #(assoc % :tags #{tag}))))

(defn tag [tag routes]
  (map (partial assoc-tag-for-route tag) routes))

(def router (ring/router
              (concat routes
                       (tag "Users" user-api/routes))
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

