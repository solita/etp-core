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

(defn wrap-ctx-fn [handler ctx]
  (fn [req]
    (handler (assoc req :ctx ctx))))

(def wrap-ctx
  {:name ::wrap-ctx-fn
   :description "Middleware for adding context from outside to every request"
   :wrap wrap-ctx-fn})

(def routes
  [["/swagger.json"
    {:get {:no-doc true
           :swagger {:info {:title "Energiatodistuspalvelu API"
                            :description ""}}
           :handler (swagger/create-swagger-handler)}}]

   ["/hello"
    {:get {:summary "Responds with message from given recipient. For testing..."
           :parameters {:query {:from s/Str}}
           :tags #{"Testing"}
           :handler #'hello-handler}}]])

(defn route-opts [ctx]
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
                       multipart/multipart-middleware
                       [wrap-ctx ctx]]}})

(defn assoc-tag-for-route [tag route]
  (update route 1 (partial map/map-values #(assoc % :tags #{tag}))))

(defn tag [tag routes]
  (map (partial assoc-tag-for-route tag) routes))

(defn router [ctx]
  (ring/router (concat routes
                       (tag "Users" user-api/routes))
               (route-opts ctx)))

(defn handler [ctx]
  (ring/ring-handler (router ctx)
                     (ring/routes
                      (swagger-ui/create-swagger-ui-handler
                       {:path "/"
                        :config {:validationUrl nil}
                        :operationsSorter "alpha"})
                      (ring/create-default-handler))))

