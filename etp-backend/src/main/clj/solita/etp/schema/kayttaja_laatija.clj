(ns solita.etp.schema.kayttaja-laatija
  (:require [solita.etp.schema.kayttaja :as kayttaja-schema]
            [solita.etp.schema.laatija :as laatija-schema]))

(def KayttajaLaatijaAdd {:kayttaja kayttaja-schema/KayttajaAdd
                         :laatija laatija-schema/LaatijaAdd})

(def KayttajaLaatija
  {:kayttaja kayttaja-schema/Kayttaja
   :laatija laatija-schema/Laatija})
