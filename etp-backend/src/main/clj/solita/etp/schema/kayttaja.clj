(ns solita.etp.schema.kayttaja
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.laatija :as laatija-schema]))

(def Whoami (merge common-schema/Id {:username schema/Str}))

(def Kayttaja
  "Schema for käyttäjä without laatija"
  (merge {:etunimi       schema/Str
          :sukunimi      schema/Str
          :email         schema/Str
          :puhelin       schema/Str
          :passivoitu    schema/Bool
          :rooli         common-schema/Key
          :login         common-schema/Date
          :ensitallennus schema/Bool
          :cognitoid     schema/Str}
         common-schema/Id))
