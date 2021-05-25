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
                               :percentile-15 common-schema/NonNegative}}
   :common-averages {:ilmanvuotoluku common-schema/NonNegative
                     :ulkoseinat-u common-schema/NonNegative
                     :ylapohja-u common-schema/NonNegative
                     :alapohja-u common-schema/NonNegative
                     :ikkunat-u common-schema/NonNegative
                     :ulkoovet-u common-schema/NonNegative
                     :takka common-schema/NonNegative
                     :ilmalampopumppu common-schema/NonNegative
                     :tilat-ja-iv-lampokerroin common-schema/NonNegative
                     :lammin-kayttovesi-lampokerroin common-schema/NonNegative
                     :lto-vuosihyotysuhde common-schema/NonNegative
                     :ivjarjestelma-sfp common-schema/NonNegative}})
