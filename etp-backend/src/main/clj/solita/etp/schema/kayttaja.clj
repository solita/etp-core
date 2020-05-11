(ns solita.etp.schema.kayttaja
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [schema-tools.core :as st]))

(def Whoami (merge common-schema/Id
   {:etunimi       schema/Str
    :sukunimi      schema/Str
    :email         schema/Str
    :rooli         common-schema/Key
    :cognitoid     (schema/maybe schema/Str)
    :henkilotunnus (schema/maybe common-schema/Henkilotunnus)
    :laatija       (schema/maybe common-schema/Key)}))

(def KayttajaAdd {:etunimi       schema/Str
                  :sukunimi      schema/Str
                  :email         schema/Str
                  :puhelin       schema/Str
                  :rooli        (schema/enum 1 2)})

(def KayttajaAdminUpdate
  {:passivoitu schema/Bool
   :rooli      (schema/enum 1 2)})

(def KayttajaUpdate
  (merge
    (st/optional-keys KayttajaAdminUpdate)
    {:etunimi       schema/Str
     :sukunimi      schema/Str
     :puhelin       schema/Str}))

(def Kayttaja
  "Schema representing the persistent kayttaja"
  (merge common-schema/Id
     {:login         (schema/maybe common-schema/Instant)
      :cognitoid     (schema/maybe schema/Str)
      :rooli         (schema/enum 0 1 2)
      :ensitallennus schema/Bool
      :passivoitu    schema/Bool

      :etunimi       schema/Str
      :sukunimi      schema/Str
      :puhelin       schema/Str
      :email         schema/Str}))
