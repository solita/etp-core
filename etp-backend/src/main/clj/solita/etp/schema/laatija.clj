(ns solita.etp.schema.laatija
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.geo :as geo-schema]))

(def Patevyystaso common-schema/Luokittelu)

(defn valid-muut-toimintaalueet? [toimintaalueet]
  (and (<= 0 (count toimintaalueet) 6)
       (apply distinct? toimintaalueet)))

(def MuutToimintaalueet (schema/constrained [common-schema/Key] valid-muut-toimintaalueet?))

(def PatevyydenToteaja (schema/enum "FISE" "KIINKO"))

(def Laatija
  "Schema for persistent laatija without käyttäjä"
  (merge {:kayttaja            common-schema/Key
          :henkilotunnus       common-schema/Henkilotunnus
          :patevyystaso        common-schema/Key
          :toteamispaivamaara  common-schema/Date
          :toteaja             PatevyydenToteaja
          :laatimiskielto      schema/Bool
          :toimintaalue        common-schema/Key
          :muut-toimintaalueet MuutToimintaalueet
          :julkisuus           {:puhelin schema/Bool
                                :email   schema/Bool
                                :osoite  schema/Bool}}
         geo-schema/Postiosoite
         common-schema/Id))

(def LaatijaSave (dissoc Laatija :id))
