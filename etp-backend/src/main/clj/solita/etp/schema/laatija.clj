(ns solita.etp.schema.laatija
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.geo :as geo-schema]))

(def Patevyys common-schema/Luokittelu)

(defn valid-muut-toimintaalueet? [toimintaalueet]
  (and (<= 0 (count toimintaalueet) 6)
       (apply distinct? toimintaalueet)))

(def MuutToimintaalueet (schema/constrained [common-schema/Key]
                                            valid-muut-toimintaalueet?))

(def PatevyydenToteaja (schema/enum "FISE" "KIINKO"))

;; TODO missing field for laskentaohjelmistot
;
(def Laatija
  "Laatija schema contains basic information about persistent laatija"
  (merge {:etunimi                                   schema/Str
          :sukunimi                                  schema/Str
          :henkilotunnus                             common-schema/Henkilotunnus
          :email                                     schema/Str
          :puhelin                                   schema/Str
          :patevyystaso                              common-schema/Key
          :toteamispaivamaara                        common-schema/Date
          :toteaja                                   PatevyydenToteaja
          (schema/optional-key :laatimiskielto)      schema/Bool
          (schema/optional-key :toimintaalue)        common-schema/Key
          (schema/optional-key :muut-toimintaalueet) MuutToimintaalueet
          (schema/optional-key :julkinen-puhelin)    schema/Bool
          (schema/optional-key :julkinen-email)      schema/Bool
          (schema/optional-key :ensitallennus)       schema/Bool}
         common-schema/Id
         geo-schema/Postiosoite))

(def LaatijaSave
  "This schema is used in add-laatija"
  (-> Laatija
      (dissoc :id)))

(def LaatijatSave
  [LaatijaSave])
