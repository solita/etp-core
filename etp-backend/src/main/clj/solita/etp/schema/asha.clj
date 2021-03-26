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

(def ActionInfoAction
  {:object-class         schema/Str
   :id                   schema/Int
   :version              schema/Int
   :contacting-direction schema/Str
   :name                 schema/Str
   :description          schema/Str
   :status               schema/Str
   :created              schema/Str})

(def ActionInfoResponse
  {:processing-action ActionInfoAction
   :assignee          schema/Str
   :queue             schema/Int
   :selected-decision {:decision               schema/Str
                       :next-processing-action ActionInfoAction}})