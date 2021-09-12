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

(defn complete-valvonta-related-schema [schema]
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
         :filename (schema/maybe schema/Str)
         :valvonta-id common-schema/Key
         :henkilot [(schema/maybe common-schema/Key)]
         :yritykset [(schema/maybe common-schema/Key)]))

(def OsapuoliBase
  (st/merge {:rooli-id (schema/maybe common-schema/Key)
             :rooli-description (schema/maybe common-schema/String200)
             :email (schema/maybe common-schema/String200)
             :puhelin (schema/maybe common-schema/String100)
             :toimitustapa-id (schema/maybe common-schema/Key)
             :toimitustapa-description (schema/maybe common-schema/String200)}
            (with-maybe-vals geo-schema/Postiosoite)))

(def HenkiloSave (assoc OsapuoliBase :henkilotunnus (schema/maybe common-schema/Henkilotunnus)
                                     :etunimi common-schema/String100
                                     :sukunimi common-schema/String100))
(def Henkilo (complete-valvonta-related-schema HenkiloSave))
(def HenkiloStatus Henkilo)

(def YritysSave (assoc OsapuoliBase :ytunnus (schema/maybe common-schema/Ytunnus)
                                    :nimi common-schema/String100))
(def Yritys (complete-valvonta-related-schema YritysSave))
(def YritysStatus Yritys)

(def ValvontaStatus
  (assoc Valvonta
         :last-toimenpide
         (schema/maybe (st/select-keys Toimenpide [:type-id :deadline-date]))
         :henkilot [(st/select-keys Henkilo [:id :rooli-id :etunimi :sukunimi])]
         :yritykset [(st/select-keys Yritys [:id :rooli-id :nimi])]))

(def Note
  {:id          common-schema/Key
   :author-id   common-schema/Key
   :create-time common-schema/Instant
   :description schema/Str})

(def henkilo? #(and (contains? % :etunimi) (contains? % :sukunimi)))
(def yritys? #(contains? % :nimi))

(def omistaja? #(= (:rooli-id %) 0))
(def tiedoksi? #(= (:rooli-id %) 1))
