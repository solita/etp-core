(ns solita.etp.schema.valvonta-oikeellisuus
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [schema-tools.core :as schema-tools]
            [solita.etp.schema.energiatodistus :as energiatodistus-schema]))

(def ValvontaSave
  {:pending    schema/Bool
   :valvoja-id (schema/maybe common-schema/Key)})

(def Valvonta (assoc ValvontaSave
                :id common-schema/Key
                :ongoing schema/Bool))

(def ToimenpideUpdate
  (schema-tools/optional-keys
    {:deadline-date (schema/maybe common-schema/Date)
     :template-id   (schema/maybe common-schema/Key)
     :description   (schema/maybe schema/Str)}))

(def ToimenpideAdd
  {:type-id       common-schema/Key
   :deadline-date (schema/maybe common-schema/Date)
   :template-id   (schema/maybe common-schema/Key)
   :description   (schema/maybe schema/Str)})

(def Toimenpide
  (assoc ToimenpideAdd
    :id common-schema/Key
    :energiatodistus-id common-schema/Key
    :diaarinumero (schema/maybe schema/Str)
    :author-id common-schema/Key
    :create-time common-schema/Instant
    :publish-time (schema/maybe common-schema/Instant)))

(def ValvontaStatus
  (assoc Valvonta
    :last-toimenpide (schema/maybe Toimenpide)
    :energiatodistus energiatodistus-schema/Energiatodistus))