(ns solita.etp.schema.kayttaja-laatija
  (:require [schema.core :as schema]
            [schema-tools.core :as st]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.kayttaja :as kayttaja-schema]
            [solita.etp.schema.laatija :as laatija-schema]))

(def Whoami (merge common-schema/Id
                   {:etunimi       schema/Str
                    :sukunimi      schema/Str
                    :email         schema/Str
                    :rooli         common-schema/Key
                    :henkilotunnus (schema/maybe common-schema/Henkilotunnus)
                    :laatija       (schema/maybe common-schema/Key)}))

(def KayttajaLaatijaAdd (merge laatija-schema/LaatijaAdd
                               kayttaja-schema/KayttajaAdd))



(def KayttajaLaatijaUpdate (merge laatija-schema/LaatijaUpdate
                                  kayttaja-schema/KayttajaUpdate))

(def KayttajaLaatijaAddResponse
  {:kayttaja common-schema/Key
   :laatija common-schema/Key})
