(ns solita.etp.schema.geo
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]))

(def Postiosoite
  {:jakeluosoite     schema/Str
   :postinumero      schema/Str
   :postitoimipaikka schema/Str
   :maa              schema/Str})

(def Toimintaalue common-schema/Luokittelu)

(def Country (assoc common-schema/Luokittelu :id schema/Str))
