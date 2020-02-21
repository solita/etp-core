(ns solita.etp.schema.yritys
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.geo :as geo-schema]))

(def YritysSave
  "This schema is used in add-yritys and update-yritys services"
  (assoc
    geo-schema/Postiosoite
    :ytunnus   schema/Str
    :nimi      schema/Str
    :wwwosoite (schema/maybe schema/Str)
    :verkkolaskuosoite (schema/maybe schema/Str)))


(def Yritys
  "Yritys schema contains basic information about persistent yritys"
  (merge common-schema/Id YritysSave))

