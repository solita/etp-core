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

(def Virhe
  {:description schema/Str
   :type-id     common-schema/Key})

(def Tiedoksi
  {:name schema/Str
   :email (schema/maybe common-schema/Email)})

(def ToimenpideUpdate
  (schema-tools/optional-keys
    {:deadline-date (schema/maybe common-schema/Date)
     :template-id   (schema/maybe common-schema/Key)
     :description   (schema/maybe schema/Str)
     :virheet       [Virhe]
     :severity-id   (schema/maybe common-schema/Key)
     :tiedoksi      [Tiedoksi]}))

(def ToimenpideAdd
  {:type-id       common-schema/Key
   :deadline-date (schema/maybe common-schema/Date)
   :template-id   (schema/maybe common-schema/Key)
   :description   (schema/maybe schema/Str)
   :virheet       [Virhe]
   :severity-id   (schema/maybe common-schema/Key)
   :tiedoksi      [Tiedoksi]})

(def Toimenpide
  (assoc ToimenpideAdd
    :id common-schema/Key
    :energiatodistus-id common-schema/Key
    :diaarinumero (schema/maybe schema/Str)
    :author common-schema/Kayttaja
    :create-time common-schema/Instant
    :publish-time (schema/maybe common-schema/Instant)
    :filename (schema/maybe schema/Str)))

(def ValvontaStatus
  (assoc Valvonta
    :last-toimenpide
    (schema/maybe (dissoc Toimenpide
                          :author :description :virheet :tiedoksi
                          :severity-id :filename))
    :energiatodistus energiatodistus-schema/Energiatodistus))

(def Virhetyyppi
  (assoc common-schema/Luokittelu
    :ordinal schema/Int
    :description-fi schema/Str
    :description-sv schema/Str))

(def VirhetyyppiUpdate (dissoc Virhetyyppi :id))

(def Note
  {:id          common-schema/Key
   :author-id   common-schema/Key
   :create-time common-schema/Instant
   :description schema/Str})

(def laatija? #(and (contains? % :etunimi) (contains? % :sukunimi)))
(def tiedoksi? #(and (contains? % :name)))