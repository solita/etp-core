(ns solita.etp.schema.laatija
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

(def LaatijaAdd (merge geo-schema/Postiosoite
                       {:henkilotunnus      common-schema/Henkilotunnus
                        :patevyystaso       common-schema/Key
                        :toteamispaivamaara common-schema/Date
                        :toteaja            PatevyydenToteaja}))

(def LaatijaAdminUpdate
  {:patevyystaso       common-schema/Key
   :toteamispaivamaara common-schema/Date
   :toteaja            PatevyydenToteaja
   :laatimiskielto     schema/Bool})

(def LaatijaUpdate
  (merge geo-schema/Postiosoite
         (st/optional-keys LaatijaAdminUpdate)
         {:henkilotunnus                            common-schema/Henkilotunnus
          :toimintaalue                             (schema/maybe common-schema/Key)
          :muuttoimintaalueet                       MuutToimintaalueet
          :julkinenpuhelin                          schema/Bool
          :julkinenemail                            schema/Bool
          :julkinenosoite                           schema/Bool}))

(def Laatija
  "Schema representing the persistent laatija"
  (merge LaatijaUpdate
         {:kayttaja common-schema/Key}
         common-schema/Id))

(def KayttajaAdd {:etunimi       schema/Str
                  :sukunimi      schema/Str
                  :email         schema/Str
                  :puhelin       schema/Str})

(def KayttajaUpdate (dissoc KayttajaAdd :email))

(def KayttajaLaatijaAdd (merge LaatijaAdd
                               KayttajaAdd))



(def KayttajaLaatijaUpdate (merge LaatijaUpdate
                                  KayttajaUpdate))

