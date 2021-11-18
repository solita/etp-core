(ns solita.etp.schema.audit
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]))

(def Audit
  {;:event-id common-schema/Key
   ;:transaction-id common-schema/Key
   :modifytime common-schema/Instant
   :modifiedby-name schema/Str
   ;;:service-uri schema/Str
   })
