(ns solita.etp.schema.kayttaja-laatija
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.kayttaja :as kayttaja-schema]
            [solita.etp.schema.laatija :as laatija-schema]))

(def KayttajaLaatijaAdd {:kayttaja kayttaja-schema/KayttajaAdd
                         :laatija laatija-schema/LaatijaAdd})

(def KayttajaLaatija
  {:kayttaja kayttaja-schema/Kayttaja
   :laatija (schema/maybe laatija-schema/Laatija)})

(def KayttajaLaatijaResponse
  {:kayttaja common-schema/Key
   :laatija common-schema/Key})
