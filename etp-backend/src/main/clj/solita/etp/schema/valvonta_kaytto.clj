(ns solita.etp.schema.valvonta-kaytto
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.geo :as geo-schema]))

(def ValvontaSave
  {:rakennustunnus (schema/maybe common-schema/Rakennustunnus)
   :katuosoite common-schema/String200
   :postinumero (schema/maybe geo-schema/PostinumeroFI)
   :ilmoituspaikka-id (schema/maybe common-schema/Key)
   :ilmoituspaikka-description (schema/maybe common-schema/String100)
   :ilmoitustunnus (schema/maybe common-schema/String100)
   :havaintopaiva (schema/maybe common-schema/Date)
   :valvoja-id (schema/maybe common-schema/Key)})

(def Valvonta (assoc ValvontaSave :id common-schema/Key))

(def ValvontaStatus Valvonta)
