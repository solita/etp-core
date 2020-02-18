(ns solita.etp.schema.laatija
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.geo :as geo-schema]))

(def Patevyys
  {:id schema/Int
   :label schema/Str})

(defn valid-muut-toimitaalueet? [toimintaalueet]
  (and (<= 0 (count toimintaalueet) 6)
       (apply distinct? toimintaalueet)))

(def MuutToimintaalueet (schema/constrained [schema/Int]
                                            valid-muut-toimitaalueet?))

;; TODO missing fields for voimassaoloaika and laskentaohjelmistot
(def LaatijaSave
  "This schema is used in add-laatija and update-laatija services"
  (merge {:etunimi                schema/Str
          :sukunimi               schema/Str
          :henkilotunnus          common-schema/Hetu
          :email                  schema/Str
          :puhelin                schema/Str
          :patevyys               schema/Int
          :patevyys-voimassa      schema/Bool
          :paa-toimintaalue       schema/Int
          :muut-toimintaalueet    MuutToimintaalueet
          :julkinen-puhelin       schema/Bool
          :julkinen-email         schema/Bool
          :ensitallennus          schema/Bool}
         geo-schema/Postiosoite))

(def Laatija
  "Laatija schema contains basic information about persistent laatija"
  (assoc LaatijaSave :id schema/Num))
