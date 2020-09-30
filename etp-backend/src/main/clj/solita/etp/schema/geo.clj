(ns solita.etp.schema.geo
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]))

(defn valid-maa? [maa] (= (count maa) 2))

(def Maa (schema/constrained schema/Str valid-maa? "country-code"))

(def Postiosoite {:jakeluosoite            schema/Str
                  :vastaanottajan-tarkenne (schema/maybe schema/Str)
                  :postinumero             schema/Str
                  :postitoimipaikka        schema/Str
                  :maa                     Maa})
(def PostinumeroFI
  (schema/constrained schema/Str #(re-find #"\d{5}" %) "FI postal code"))

(def Toimintaalue common-schema/Luokittelu)

(def Country (assoc common-schema/Luokittelu :id schema/Str))
