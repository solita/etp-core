(ns solita.etp.schema.kayttaja
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]))

(def Whoami (merge common-schema/Id {:username schema/Str}))

(def KayttajaAdd {:etunimi       schema/Str
                  :sukunimi      schema/Str
                  :email         schema/Str
                  :puhelin       schema/Str})

(def KayttajaUpdate (merge KayttajaAdd
                           {:passivoitu schema/Bool
                            :rooli      common-schema/Key}))

(def Kayttaja
  "Schema representing the persistent kayttaja"
  (merge KayttajaUpdate
         common-schema/Id
         {:login         (schema/maybe common-schema/Date)
          :ensitallennus schema/Bool
          :cognitoid     (schema/maybe schema/Str)}))

(def Rooli common-schema/Luokittelu)
