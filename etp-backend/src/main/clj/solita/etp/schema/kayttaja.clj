(ns ^{:doc
      "Schemas for all users (kayttaja) or schemas for other users than laatija.
       Schemas specific only for laatija are in laatija namespace."}
  solita.etp.schema.kayttaja
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.audit :as audit-schema]
            [schema-tools.core :as st]))

(def VirtuId
  {:localid schema/Str
   :organisaatio schema/Str})

(def KayttajaAdminUpdate
  "Only administrators can update this information.
   Not intended for laatija-users."
  {:passivoitu schema/Bool
   :rooli      (schema/enum 1 2 3)
   :henkilotunnus (schema/maybe common-schema/Henkilotunnus)
   :virtu (schema/maybe VirtuId)})

(def KayttajaUpdate
  "Schema to update all other users (kayttaja) except laatija."
  (merge
    (st/optional-keys KayttajaAdminUpdate)
    {:etunimi       schema/Str
     :sukunimi      schema/Str
     :email         schema/Str
     :puhelin       schema/Str}))

(def Kayttaja
  "Schema representing any persistent kayttaja (any role)"
  (merge common-schema/Id
         {:login         (schema/maybe common-schema/Instant)
          :cognitoid     (schema/maybe schema/Str)
          :virtu         (schema/maybe VirtuId)
          :henkilotunnus (schema/maybe common-schema/Henkilotunnus)
          :rooli         (schema/enum 0 1 2 3)
          :verifytime    (schema/maybe common-schema/Instant)
          :passivoitu    schema/Bool
          :etunimi       schema/Str
          :sukunimi      schema/Str
          :puhelin       schema/Str
          :email         schema/Str}))

(def Whoami (-> Kayttaja
                (dissoc :passivoitu :puhelin :login)
                (assoc :ispartner schema/Bool)))

(def KayttajaHistory
  (-> Kayttaja
      (merge audit-schema/Audit)
      (dissoc :login :verifytime)))
