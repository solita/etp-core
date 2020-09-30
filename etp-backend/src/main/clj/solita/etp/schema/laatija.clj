(ns ^{:doc "Schemas specific only for laatijat."}
  solita.etp.schema.laatija
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.geo :as geo-schema]
            [solita.etp.schema.kayttaja :as kayttaja-schema]
            [schema-tools.core :as st]
            [schema.core :as s]))

(def Patevyystaso common-schema/Luokittelu)

(defn valid-muut-toimintaalueet? [toimintaalueet]
  (and (<= 0 (count toimintaalueet) 6)
       (or (empty? toimintaalueet)
           (apply distinct? toimintaalueet))))

(def MuutToimintaalueet (schema/constrained [common-schema/Key] valid-muut-toimintaalueet?))

(def PatevyydenToteaja (schema/enum "FISE" "KIINKO"))

(def LaatijaAdd
  "Only for internal use in laatija services.
   Represents laatija information which is stored in laatija-table."
  (st/merge (st/select-keys geo-schema/Postiosoite [:jakeluosoite :postinumero :postitoimipaikka :maa])
            {:patevyystaso       common-schema/Key
             :toteamispaivamaara common-schema/Date
             :toteaja            PatevyydenToteaja}))

(def LaatijaAdminUpdate
  "Only for internal use in laatija services.
   Represents laatija information which can be updated by admins."
  {:patevyystaso       common-schema/Key
   :toteamispaivamaara common-schema/Date
   :toteaja            PatevyydenToteaja
   :laatimiskielto     schema/Bool})

(def LaatijaUpdate
  "Only for internal use in laatija services.
   Represents laatija information which is stored in laatija-table."
  (merge geo-schema/Postiosoite
         (st/optional-keys LaatijaAdminUpdate)
         {:toimintaalue                             (schema/maybe common-schema/Key)
          :muuttoimintaalueet                       MuutToimintaalueet
          :julkinenpuhelin                          schema/Bool
          :julkinenemail                            schema/Bool
          :julkinenosoite                           schema/Bool
          :julkinenwwwosoite                        schema/Bool
          :wwwosoite                                (schema/maybe schema/Str)
          :laskutuskieli                            (schema/enum 0 1 2)}))

(def Laatija
  "Schema representing the persistent laatija.
  This defines only the laatija specific kayttaja information."
  (merge LaatijaUpdate
         common-schema/Id
         {:henkilotunnus common-schema/Henkilotunnus}))

(def KayttajaAdminUpdate
  "Only for internal use in laatija services.
   Represents kayttaja information which can be updated by admins."
  {:etunimi  schema/Str
   :sukunimi schema/Str
   :henkilotunnus common-schema/Henkilotunnus})

(def KayttajaUpdate
  "Only for internal use in laatija services.
   Represents kayttaja information which is stored in kayttaja-table."
  (merge (st/optional-keys KayttajaAdminUpdate)
         {:email   schema/Str
          :puhelin schema/Str}))

(def KayttajaAdd (st/required-keys KayttajaUpdate))

(def KayttajaLaatijaAdd
  "A schema for adding new or updating existing laatija.
  Contains all laatija user information."
  (merge LaatijaAdd
         KayttajaAdd))

(def KayttajaLaatijaUpdate
  "A schema for updating an existing laatija.
  Contains all laatija user information."
  (merge LaatijaUpdate
         KayttajaUpdate))

(def always-public-kayttaja-laatija-ks
  [:etunimi :sukunimi :patevyystaso :toteamispaivamaara :toimintaalue :muuttoimintaalueet :voimassa])

(def LaatijaFind
  "A schema for find all existing laatija"
  (-> (st/merge Laatija kayttaja-schema/Kayttaja)
      (st/assoc :voimassa schema/Bool)
      (st/assoc :henkilotunnus schema/Str)
      (st/assoc :yritys [common-schema/Key])

      ;; Pätevyydentoteajat do not see the last part of hetu
      (st/optional-keys)
      (st/required-keys always-public-kayttaja-laatija-ks)))
