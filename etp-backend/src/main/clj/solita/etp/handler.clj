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
            [schema.core :as s]))

(defn hello-handler [{:keys [parameters]}]
  {:status 200
   :body {:msg (str "Hello from" (-> parameters :query :from) "!")
          :parameters parameters}})

(def routes
  [["/swagger.json"
    {:get {:no-doc true
           :swagger {:info {:title "Energiatodistuspalvelu API"
                            :description ""}}
           :handler (swagger/create-swagger-handler)}}]

   ["/hello"
    {:get {:summary "Responds with message from given recipient. For testing..."
           :parameters {:qquery {:from s/Str}}
           :swagger {:tags "hello"}
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

(def router
  (ring/router routes route-opts))

(def handler
  (ring/ring-handler router
                     (ring/routes
                      (swagger-ui/create-swagger-ui-handler
                       {:path "/"
                        :config {:validationUrl nil}
                        :operationsSorter "alpha"})
                      (ring/create-default-handler))))

