(ns solita.etp.schema.laatija
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.geo :as geo-schema]))

(def Patevyys (merge common-schema/Id {:label schema/Str}))

(defn valid-muut-toimintaalueet? [toimintaalueet]
  (and (<= 0 (count toimintaalueet) 6)
       (apply distinct? toimintaalueet)))

(def MuutToimintaalueet (schema/constrained [common-schema/Key]
                                            valid-muut-toimintaalueet?))

;; TODO missing fields for voimassaoloaika and laskentaohjelmistot
(def LaatijaSave
  "This schema is used in add-laatija and update-laatija services"
  (merge {:etunimi                schema/Str
          :sukunimi               schema/Str
          :henkilotunnus          common-schema/Henkilotunnus
          :email                  schema/Str
          :puhelin                schema/Str
          :patevyys               common-schema/Key
          :patevyys-voimassa      schema/Bool
          :toimintaalue           common-schema/Key
          :muut-toimintaalueet    MuutToimintaalueet
          :julkinen-puhelin       schema/Bool
          :julkinen-email         schema/Bool
          :ensitallennus          schema/Bool}
         geo-schema/Postiosoite))

(def Laatija
  "Laatija schema contains basic information about persistent laatija"
  (merge common-schema/Id LaatijaSave))
