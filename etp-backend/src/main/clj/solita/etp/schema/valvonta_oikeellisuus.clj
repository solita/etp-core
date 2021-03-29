(ns solita.etp.schema.valvonta-oikeellisuus
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [schema-tools.core :as schema-tools]
            [solita.etp.schema.energiatodistus :as energiatodistus-schema]))

(def ValvontaSave
  {:active       schema/Bool
   :liitteet     schema/Bool
   :valvoja-id   common-schema/Key})

(def ToimenpideUpdate
  (schema-tools/optional-keys
    {:deadline-date (schema/maybe common-schema/Date)
     :document      (schema/maybe schema/Str)}))

(def ToimenpideAdd
  {:type-id       common-schema/Key
   :deadline-date (schema/maybe common-schema/Date)
   :document      (schema/maybe schema/Str)})

(def Toimenpide
  (assoc ToimenpideAdd
    :id common-schema/Key
    :energiatodistus-id common-schema/Key
    :diaarinumero (schema/maybe schema/Str)
    :author-id common-schema/Key
    :create-time common-schema/Instant
    :publish-time (schema/maybe common-schema/Instant)))

(def Valvonta
  (assoc Toimenpide :energiatodistus energiatodistus-schema/Energiatodistus))