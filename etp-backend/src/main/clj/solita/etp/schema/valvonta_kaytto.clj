(ns solita.etp.schema.valvonta-kaytto
  (:require [schema.core :as schema]
            [schema-tools.core :as st]
            [schema-tools.walk :as walk]
            [solita.common.schema :as xschema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.geo :as geo-schema]
            [schema-tools.core :as schema-tools]))

(defn with-maybe-vals [schema]
  (walk/prewalk (fn [x]
                  (if (and (map-entry? x) (-> x second xschema/maybe? not))
                    (clojure.lang.MapEntry. (first x) (schema/maybe (second x)))
                    x))
                geo-schema/Postiosoite))

(defn complete-valvonta-schema [schema]
  (assoc schema :id common-schema/Key :valvonta-id common-schema/Key))

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

(def OsapuoliBase
  (st/merge {:nimi common-schema/String100
             :rooli-id (schema/maybe common-schema/Key)
             :rooli-description (schema/maybe common-schema/String200)
             :email (schema/maybe common-schema/String200)
             :puhelin (schema/maybe common-schema/String100)
             :toimitustapa-id (schema/maybe common-schema/Key)
             :toimitustapa-description (schema/maybe common-schema/String200)}
            (with-maybe-vals geo-schema/Postiosoite)))

(def HenkiloSave (assoc OsapuoliBase :henkilotunnus common-schema/Henkilotunnus))
(def Henkilo (complete-valvonta-schema HenkiloSave))
(def HenkiloStatus Henkilo)

(def YritysSave (assoc OsapuoliBase :ytunnus common-schema/Ytunnus))
(def Yritys (complete-valvonta-schema YritysSave))
(def YritysStatus Yritys)

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
    :diaarinumero (schema/maybe schema/Str)
    :author common-schema/Kayttaja
    :create-time common-schema/Instant
    :publish-time common-schema/Instant
    :filename schema/Str))
