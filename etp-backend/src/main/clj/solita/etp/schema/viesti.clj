(ns solita.etp.schema.viesti
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]))

(def KetjuAdd
  "Uuden viestiketjun luontitiedot"
  {:vastaanottajat        [common-schema/Key]
   :vastaanottajaryhma-id (schema/maybe common-schema/Key)
   :energiatodistus-id    (schema/maybe common-schema/Key)
   :subject               schema/Str
   :body                  schema/Str})

(def KetjuUpdate
  {(schema/optional-key :kasittelija-id) (schema/maybe common-schema/Key)
   (schema/optional-key :energiatodistus-id) (schema/maybe common-schema/Key)
   (schema/optional-key :kasitelty) schema/Bool})

(def Kayttaja
  "Sender or Recipient information only, not the full Kayttaja"
  {:id       common-schema/Key
   :rooli-id common-schema/Key
   :sukunimi schema/Str
   :etunimi  schema/Str})

(def Viesti
  {:from      Kayttaja
   :sent-time common-schema/Instant
   :read-time (schema/maybe common-schema/Instant)
   :body      schema/Str})

(def Ketju
  {:id                    common-schema/Key
   :kasittelija-id        (schema/maybe common-schema/Key)
   :kasitelty             schema/Bool
   :vastaanottajat        [Kayttaja]
   :vastaanottajaryhma-id (schema/maybe common-schema/Key)
   :energiatodistus-id    (schema/maybe common-schema/Key)
   :subject               schema/Str
   :viestit               [Viesti]})

(def KetjuQueryWindow
  {(schema/optional-key :limit)  (common-schema/LimitedInt 1 100)
   (schema/optional-key :offset) schema/Int})

(def KetjuQuery
  {(schema/optional-key :kasittelija-id)    common-schema/Key
   (schema/optional-key :has-kasittelija)   schema/Bool
   (schema/optional-key :include-kasitelty) schema/Bool})
