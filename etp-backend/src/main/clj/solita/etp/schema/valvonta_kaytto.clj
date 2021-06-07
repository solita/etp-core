(ns solita.etp.schema.valvonta-kaytto
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.geo :as geo-schema]))

;; TODO which fields are mandatory?
(def ValvontaSave
  {:rakennustunnus (schema/maybe common-schema/Rakennustunnus)
   :katuosoite (schema/maybe common-schema/String100)
   :postinumero (schema/maybe geo-schema/PostinumeroFI)
   :ilmoituspaikka (schema/maybe common-schema/Key)
   :ilmoitustunnus (schema/maybe common-schema/String100)
   :ilmoitus-katsomispaiva (schema/maybe common-schema/Date)
   :valvoja-id (schema/maybe common-schema/Key)})

(def Valvonta (assoc ValvontaSave :id common-schema/Key))

(def ValvontaStatus Valvonta)
