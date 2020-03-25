(ns solita.etp.schema.geo
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]))

(defn valid-maa? [maa] (= (count maa) 2))

(def Postiosoite {:jakeluosoite     schema/Str
                  :postinumero      schema/Str
                  :postitoimipaikka schema/Str
                  :maa              (schema/constrained schema/Str valid-maa?)})
(def Postinumero
  (schema/constrained schema/Str #(re-find #"\d{5}" %)))

(def Toimintaalue common-schema/Luokittelu)

(def Country (assoc common-schema/Luokittelu :id schema/Str))
