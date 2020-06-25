(ns solita.etp.schema.yritys
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.geo :as geo-schema]))

(def YritysSave
  "This schema is used in add-yritys and update-yritys services"
  (assoc
    geo-schema/Postiosoite
    :ytunnus                common-schema/Ytunnus
    :nimi                   schema/Str
    :verkkolaskuoperaattori (schema/maybe common-schema/Key)
    :verkkolaskuosoite      (schema/maybe schema/Str)
    :laskutuskieli          (schema/enum 0 1 2)))

(def Yritys
  "Yritys schema contains basic information about persistent yritys"
  (merge common-schema/Id YritysSave))

(def Verkkolaskuoperaattori
  (merge common-schema/Id {:valittajatunnus schema/Str
                           :nimi            schema/Str}))
