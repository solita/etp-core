(ns ^{:doc (str "Schemas specific only for laatijat.")}
  solita.etp.schema.laatija
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.geo :as geo-schema]
            [schema-tools.core :as st]))

(def Patevyystaso common-schema/Luokittelu)

(defn valid-muut-toimintaalueet? [toimintaalueet]
  (and (<= 0 (count toimintaalueet) 6)
       (or (empty? toimintaalueet)
           (apply distinct? toimintaalueet))))

(def MuutToimintaalueet (schema/constrained [common-schema/Key] valid-muut-toimintaalueet?))

(def PatevyydenToteaja (schema/enum "FISE" "KIINKO"))

(def LaatijaAdd
  (str "Only for internal use in laatija services. "
       "Represents laatija information which is stored in laatija-table.")
  (merge geo-schema/Postiosoite
     {:henkilotunnus      common-schema/Henkilotunnus
      :patevyystaso       common-schema/Key
      :toteamispaivamaara common-schema/Date
      :toteaja            PatevyydenToteaja}))

(def LaatijaAdminUpdate
  (str "Only for internal use in laatija services. "
       "Represents laatija information which can be updated by admins.")
  {:patevyystaso       common-schema/Key
   :toteamispaivamaara common-schema/Date
   :toteaja            PatevyydenToteaja
   :laatimiskielto     schema/Bool})

(def LaatijaUpdate
  (str "Only for internal use in laatija services. "
       "Represents laatija information which is stored in laatija-table.")
  (merge geo-schema/Postiosoite
         (st/optional-keys LaatijaAdminUpdate)
         {:henkilotunnus                            common-schema/Henkilotunnus
          :toimintaalue                             (schema/maybe common-schema/Key)
          :muuttoimintaalueet                       MuutToimintaalueet
          :julkinenpuhelin                          schema/Bool
          :julkinenemail                            schema/Bool
          :julkinenosoite                           schema/Bool}))

(def Laatija
  (str "Schema representing the persistent laatija."
  "This defines only the laatija specific kayttaja information.")
  (merge LaatijaUpdate
         {:kayttaja common-schema/Key}
         common-schema/Id))

(def KayttajaAdd
  (str "Only for internal use in laatija services. "
       "Represents laatija information which is stored in kayttaja-table.")
  {:etunimi  schema/Str
   :sukunimi schema/Str
   :email    schema/Str
   :puhelin  schema/Str})

(def KayttajaUpdate
  (str "Only for internal use in laatija services. "
       "Represents laatija information which is stored in kayttaja-table.")
  (dissoc KayttajaAdd :email))

(def KayttajaLaatijaAdd
  "A schema for upserting new or existing laatija."
  (merge LaatijaAdd
         KayttajaAdd))

(def KayttajaLaatijaUpdate
  "A schema for updating an existing laatija."
  (merge LaatijaUpdate
         KayttajaUpdate))

