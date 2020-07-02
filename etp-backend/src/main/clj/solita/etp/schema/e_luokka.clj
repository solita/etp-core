(ns solita.etp.schema.e-luokka
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]))

(def ELuokka {:e-luokka schema/Str
              :luokittelu common-schema/Luokittelu
              :limits [schema/Any]})
