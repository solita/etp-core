(ns solita.etp.schema.viesti
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]))

(def KetjuAdd
  "Uuden viestiketjun luontitiedot"
  {:kayttajat          [common-schema/Key]
   :kayttajarooli-id   (schema/maybe common-schema/Key)
   :kayttajaryhma-id   (schema/maybe common-schema/Key)
   :energiatodistus-id (schema/maybe common-schema/Key)
   :subject            schema/Str
   :body               schema/Str})

(def Sender
  {:id       common-schema/Key
   :rooli-id common-schema/Key
   :sukunimi schema/Str
   :etunimi  schema/Str})

(def Viesti
  {:from     Sender
   :senttime common-schema/Instant
   :body     schema/Str})

(def Ketju
  {:id                 common-schema/Key
   :kayttajat          [common-schema/Key]
   :kayttajarooli-id   (schema/maybe common-schema/Key)
   :kayttajaryhma-id   (schema/maybe common-schema/Key)
   :energiatodistus-id (schema/maybe common-schema/Key)
   :subject            schema/Str
   :viestit            [Viesti]})