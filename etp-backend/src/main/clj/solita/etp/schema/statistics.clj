(ns solita.etp.schema.statistics
  (:require [schema.core :as schema]
            [schema-tools.core :as st]
            [solita.etp.schema.common :as common-schema]))

(def StatisticsQuery
  (st/optional-keys
   {:postinumero schema/Str
    :kunta schema/Str
    :alakayttotarkoitus-ids [schema/Str]
    :valmistumisvuosi-min common-schema/Year
    :valmistumisvuosi-max common-schema/Year
    :lammitetty-nettoala-min common-schema/NonNegative
    :lammitetty-nettoala-max common-schema/NonNegative}))

(def Versio (schema/enum 2013 2018))

(def StatisticsResponse
  {:e-luokka-counts {Versio {schema/Str common-schema/NonNegative}}
   :e-luku-statistics {Versio {:avg common-schema/NonNegative
                               :min common-schema/NonNegative
                               :percentile-15 common-schema/NonNegative}}})
