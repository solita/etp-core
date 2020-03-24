(ns solita.etp.api.energiatodistus
  (:require [ring.util.response :as r]
            [solita.etp.schema.energiatodistus :as energiatodistus]
            [schema.core :as schema]))

(def routes
  [["/energiatodistus/"
    {:post {:summary    "Lisää luonnostilaisen energiatodistuksen"
            :parameters {:body energiatodistus/Energiatodistus}
            :responses  {200 {:body {:ok schema/Str}}}
            :handler    (fn []
                          (r/response {:ok "ok"}))}}]])