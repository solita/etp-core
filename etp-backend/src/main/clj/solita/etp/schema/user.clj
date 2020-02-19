(ns solita.etp.schema.user
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]))

(def User (merge common-schema/Id {:username schema/Str}))
