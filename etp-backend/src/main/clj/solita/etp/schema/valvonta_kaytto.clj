(ns solita.etp.schema.valvonta-kaytto
  (:require [schema.core :as schema]
            [schema-tools.core :as st]
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

(def HenkiloSave
  (st/merge {:nimi common-schema/String100
             :henkilotunnus common-schema/Henkilotunnus
             :rooli-id common-schema/Key
             :rooli-description common-schema/String200
             :email common-schema/String200
             :puhelin common-schema/String100
             :toimitustapa-id common-schema/Key
             :toimitustapa-description common-schema/String200}
            geo-schema/Postiosoite))

(def Henkilo (assoc HenkiloSave
                    :id
                    common-schema/Key
                    :valvonta-id
                    common-schema/Key))

(def HenkiloStatus Henkilo)
