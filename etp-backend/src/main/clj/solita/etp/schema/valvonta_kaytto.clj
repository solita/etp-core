(ns solita.etp.schema.valvonta-kaytto
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.geo :as geo-schema]
            [solita.etp.schema.valvonta :as valvonta-schema]
            [schema-tools.core :as schema-tools]))

(defn complete-valvonta-related-schema [schema]
  (assoc schema :id common-schema/Key :valvonta-id common-schema/Key))

(def ValvontaQuery
  {(schema/optional-key :valvoja-id) common-schema/Key
   (schema/optional-key :has-valvoja) schema/Bool
   (schema/optional-key :include-closed) schema/Bool
   (schema/optional-key :keyword) schema/Str
   (schema/optional-key :toimenpidetype-id) common-schema/Key})

(def ValvontaSave
  {:rakennustunnus             (schema/maybe common-schema/Rakennustunnus)
   :katuosoite                 common-schema/String200
   :postinumero                (schema/maybe geo-schema/PostinumeroFI)
   :ilmoituspaikka-id          (schema/maybe common-schema/Key)
   :ilmoituspaikka-description (schema/maybe common-schema/String100)
   :ilmoitustunnus             (schema/maybe common-schema/String100)
   :havaintopaiva              (schema/maybe common-schema/Date)
   :valvoja-id                 (schema/maybe common-schema/Key)})

(def Valvonta (assoc ValvontaSave :id common-schema/Key))

(def ToimenpideUpdate
  (schema-tools/optional-keys
    {:deadline-date (schema/maybe common-schema/Date)
     :template-id   (schema/maybe common-schema/Key)
     :description   (schema/maybe schema/Str)}))

(def ToimenpideAdd
  {:type-id                           common-schema/Key
   :deadline-date                     (schema/maybe common-schema/Date)
   :template-id                       (schema/maybe common-schema/Key)
   :description                       (schema/maybe schema/Str)
   (schema/optional-key :bypass-asha) schema/Bool
   (schema/optional-key :fine)        common-schema/NonNegative})

(def Template
  (assoc valvonta-schema/Template
         :tiedoksi schema/Bool))

(def OsapuoliBase
  (schema-tools/merge {:rooli-id                 (schema/maybe common-schema/Key)
                       :rooli-description        (schema/maybe common-schema/String200)
                       :email                    (schema/maybe common-schema/String200)
                       :puhelin                  (schema/maybe common-schema/String100)
                       :toimitustapa-id          (schema/maybe common-schema/Key)
                       :toimitustapa-description (schema/maybe common-schema/String200)}
                      (common-schema/with-maybe-vals geo-schema/Postiosoite)))

(def HenkiloSave (assoc OsapuoliBase :henkilotunnus (schema/maybe common-schema/Henkilotunnus)
                                     :etunimi common-schema/String100
                                     :sukunimi common-schema/String100))
(def Henkilo (complete-valvonta-related-schema HenkiloSave))
(def HenkiloStatus Henkilo)

(def YritysSave (assoc OsapuoliBase :ytunnus (schema/maybe common-schema/Ytunnus)
                                    :nimi common-schema/String100))
(def Yritys (complete-valvonta-related-schema YritysSave))
(def YritysStatus Yritys)

(def Toimenpide
  (-> ToimenpideAdd
      (dissoc (schema/optional-key :bypass-asha))
      (assoc
       :id common-schema/Key
       :diaarinumero (schema/maybe schema/Str)
       :author common-schema/Kayttaja
       :create-time common-schema/Instant
       :publish-time common-schema/Instant
       :filename (schema/maybe schema/Str)
       :valvonta-id common-schema/Key
       :henkilot [Henkilo]
       :yritykset [Yritys])))

(def LastToimenpide
  (schema-tools/select-keys Toimenpide
                            [:id :diaarinumero :type-id
                             :deadline-date :create-time
                             :publish-time :template-id]))

(def ValvontaStatus
  (assoc Valvonta
    :last-toimenpide
    (schema/maybe LastToimenpide)
    :energiatodistus
    (schema/maybe {:id common-schema/Key})
    :henkilot [Henkilo]
    :yritykset [Yritys]))

(def Note
  {:id          common-schema/Key
   :author-id   common-schema/Key
   :create-time common-schema/Instant
   :description schema/Str})

(def ExistingValvonta
  {:id common-schema/Key
   :end-time (schema/maybe common-schema/Instant)})

(def Toimenpidetyypit
  (assoc common-schema/Luokittelu
    :manually-deliverable
    schema/Bool
    :allow-comments
    schema/Bool))