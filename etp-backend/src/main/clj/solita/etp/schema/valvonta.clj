(ns solita.etp.schema.valvonta
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]))

(def Valvonta {:active schema/Bool})

(def ValvontaQueryWindow (common-schema/QueryWindow 100))

(def ValvontaQuery
  {(schema/optional-key :valvoja-id) common-schema/Key})

(def Template
  (assoc common-schema/Luokittelu
    :toimenpidetype-id common-schema/Key
    :language schema/Str))
