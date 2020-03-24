(ns solita.etp.schema.kayttaja-laatija
  (:require [schema.core :as schema]
            [schema-tools.core :as st]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.kayttaja :as kayttaja-schema]
            [solita.etp.schema.laatija :as laatija-schema]))

(def KayttajaLaatijaAdd (merge kayttaja-schema/KayttajaAdd
                               laatija-schema/LaatijaAdd))

(def KayttajaLaatijaAddResponse
  {:kayttaja common-schema/Key
   :laatija common-schema/Key})
