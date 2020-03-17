(ns solita.etp.schema.kayttaja
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]))

(def Kayttaja (merge common-schema/Id {:username schema/Str}))
