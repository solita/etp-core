(ns solita.etp.schema.energiatodistus-history
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]))

(def AuditEvent {:modifiedby-fullname schema/Str
                 :modifytime common-schema/Instant
                 :k schema/Keyword
                 :v schema/Any
                 :type schema/Keyword
                 :external-api schema/Bool})

(def HistoryResponse
  {:state-history [AuditEvent]
   :form-history [AuditEvent]})
