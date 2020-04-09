(ns solita.etp.schema.kayttaja
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]))

(def KayttajaAdd {:etunimi       schema/Str
                  :sukunimi      schema/Str
                  :email         schema/Str
                  :puhelin       schema/Str})

(def KayttajaUpdate
  (-> KayttajaAdd
      (dissoc :email)
      (merge {(schema/optional-key :passivoitu) schema/Bool
              (schema/optional-key :rooli)      (schema/enum 0 1 2)})))

(def Kayttaja
  "Schema representing the persistent kayttaja"
  (merge KayttajaUpdate
         KayttajaAdd
         common-schema/Id
         {:login         (schema/maybe common-schema/Instant)
          :ensitallennus schema/Bool
          :cognitoid     (schema/maybe schema/Str)}))
