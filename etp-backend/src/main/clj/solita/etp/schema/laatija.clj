(ns ^{:doc "Schemas specific only for laatijat."}
  solita.etp.schema.laatija
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.geo :as geo-schema]
            [solita.etp.schema.kayttaja :as kayttaja-schema]
            [schema-tools.core :as st]))

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
  (merge geo-schema/Postiosoite
         {:henkilotunnus      common-schema/Henkilotunnus
          :patevyystaso       common-schema/Key
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
         {:henkilotunnus                            common-schema/Henkilotunnus
          :toimintaalue                             (schema/maybe common-schema/Key)
          :muuttoimintaalueet                       MuutToimintaalueet
          :julkinenpuhelin                          schema/Bool
          :julkinenemail                            schema/Bool
          :julkinenosoite                           schema/Bool
          :wwwosoite                                (schema/maybe schema/Str)}))

(def Laatija
  "Schema representing the persistent laatija.
  This defines only the laatija specific kayttaja information."
  (merge LaatijaUpdate
         common-schema/Id))

(def KayttajaAdd
  "Only for internal use in laatija services.
  Represents laatija information which is stored in kayttaja-table."
  {:etunimi  schema/Str
   :sukunimi schema/Str
   :email    schema/Str
   :puhelin  schema/Str})

(def KayttajaUpdate
  "Only for internal use in laatija services.
  Represents laatija information which is stored in kayttaja-table."
  (dissoc KayttajaAdd :email))

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

(def LaatijaFind
  "A schema for find all existing laatija"
  (st/merge
    (st/select-keys Laatija [:patevyystaso :toteamispaivamaara :toimintaalue :postinumero :laatimiskielto])
    (st/select-keys kayttaja-schema/Kayttaja [:id :etunimi :sukunimi :puhelin])
    {:yritys [common-schema/Key]
     :voimassa schema/Bool}))
