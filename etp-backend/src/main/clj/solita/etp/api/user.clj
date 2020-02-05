(ns solita.etp.api.user
  (:require [schema.core :as schema]
            [solita.etp.schema.user :as user-schema]
            [ring.util.response :as r]))

(def routes
  [["/users/current"
    {:get {:summary "Current user information"
           :responses {200 {:body user-schema/User}}
           :handler (constantly (r/response {:id 1234 :username "Testi"}))}}]])