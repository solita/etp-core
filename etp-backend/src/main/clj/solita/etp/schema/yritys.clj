(ns solita.etp.schema.yritys
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.geo :as geo-schema]))

(def YritysSave
  "This schema is used in add-yritys and update-yritys services"
  {:ytunnus   schema/Str
   :nimi      schema/Str
   :wwwosoite schema/Str})

(def Yritys
  "Yritys schema contains basic information about persistent yritys"
  (merge common-schema/Id YritysSave))

(def LaskutusosoiteSave
  (merge geo-schema/Postiosoite {:verkkolaskuosoite (schema/maybe schema/Str)}))

(def Laskutusosoite
  "Yritys schema contains basic information about persistent yritys"
  (merge common-schema/Id LaskutusosoiteSave {:yritysid common-schema/Key}))
