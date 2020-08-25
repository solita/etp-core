(ns solita.etp.schema.whoami
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]))

(def Whoami (merge common-schema/Id
                   {:etunimi       schema/Str
                    :sukunimi      schema/Str
                    :email         schema/Str
                    :rooli         common-schema/Key
                    :cognitoid     (schema/maybe schema/Str)
                    :henkilotunnus (schema/maybe common-schema/Henkilotunnus)}))
