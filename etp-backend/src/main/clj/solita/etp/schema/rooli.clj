(ns solita.etp.schema.rooli
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]))

(def Rooli (assoc common-schema/Luokittelu :k schema/Keyword))
