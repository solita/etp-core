(ns solita.etp.schema.yritys
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]))

(def YritysSave
  "This schema is used in add-yritys and update-yritys services"
  {:ytunnus   schema/Str
   :nimi      schema/Str
   :wwwosoite schema/Str})

(def Yritys
  "Yritys schema contains basic information about persistent yritys"
  (assoc YritysSave :id schema/Num))

(def LaskutusosoiteSave
  (merge {(schema/optional-key :verkkolaskuosoite) schema/Str}
         common-schema/Postiosoite))

(def Laskutusosoite
  "Yritys schema contains basic information about persistent yritys"
  (assoc LaskutusosoiteSave :id schema/Num :yritysid schema/Num))
