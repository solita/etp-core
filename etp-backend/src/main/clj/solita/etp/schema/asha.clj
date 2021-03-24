(ns solita.etp.schema.asha
  (:require [schema.core :as schema]))

(def CaseCreateResponse
  {:id          schema/Int
   :case-number schema/Str})

(def CaseInfoResponse
  {:id schema/Int
   :case-number schema/Str
   :status schema/Str
   :classification schema/Str
   :name schema/Str
   :description schema/Str
   :created schema/Str})