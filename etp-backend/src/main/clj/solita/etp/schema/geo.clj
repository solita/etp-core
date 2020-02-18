(ns solita.etp.schema.geo
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]))

(def Postiosoite
  {:jakeluosoite        schema/Str
   :postinumero         schema/Str
   :postitoimipaikka    schema/Str
   :maa                 schema/Str})

(def Maakunta
  {:id schema/Int
   :label schema/Str})
