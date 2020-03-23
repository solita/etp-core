(ns solita.etp.schema.geo
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]))

(defn valid-maa? [maa] (= (count maa) 2))

(def PostiosoiteWithoutMaa {:jakeluosoite     schema/Str
                            :postinumero      schema/Str
                            :postitoimipaikka schema/Str})

(def Postiosoite (assoc PostiosoiteWithoutMaa
                        :maa
                        (schema/constrained schema/Str valid-maa?)))

(def Toimintaalue common-schema/Luokittelu)

(def Country (assoc common-schema/Luokittelu :id schema/Str))
