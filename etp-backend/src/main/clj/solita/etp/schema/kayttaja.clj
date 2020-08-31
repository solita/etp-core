(ns ^{:doc
      "Schemas for all users (kayttaja) or schemas for other users than laatija.
       Schemas specific only for laatija are in laatija namespace."}
  solita.etp.schema.kayttaja
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [schema-tools.core :as st]))

(def KayttajaAdd
  "Schema to add all other users (kayttaja) except laatija."
  {:etunimi  schema/Str
   :sukunimi schema/Str
   :email    schema/Str
   :puhelin  schema/Str
   :rooli    (schema/enum 1 2)})

(def KayttajaAdminUpdate
  "Only administrators can update this information.
   Not intended for laatija-users."
  {:passivoitu schema/Bool
   :rooli      (schema/enum 1 2)
   :henkilotunnus (schema/maybe common-schema/Henkilotunnus)})

(def KayttajaUpdate
  "Schema to update all other users (kayttaja) except laatija."
  (merge
    (st/optional-keys KayttajaAdminUpdate)
    {:etunimi       schema/Str
     :sukunimi      schema/Str
     :puhelin       schema/Str}))

(def Kayttaja
  "Schema representing any persistent kayttaja (any role)"
  (merge common-schema/Id
         {:login             (schema/maybe common-schema/Instant)
          :cognitoid         (schema/maybe schema/Str)
          :virtuid           (schema/maybe schema/Str)
          :virtuorganisaatio (schema/maybe schema/Str)
          :henkilotunnus     (schema/maybe common-schema/Henkilotunnus)
          :rooli             (schema/enum 0 1 2)
          :ensitallennus     schema/Bool
          :passivoitu        schema/Bool
          :etunimi           schema/Str
          :sukunimi          schema/Str
          :puhelin           schema/Str
          :email             schema/Str}))
