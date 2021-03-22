(ns solita.etp.schema.asha
  (:require [schema.core :as schema]))

(def CaseCreateResponse
  {:id          schema/Int
   :case-number schema/Str})