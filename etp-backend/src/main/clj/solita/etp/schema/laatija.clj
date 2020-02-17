(ns solita.etp.schema.laatija
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]))

(def Patevyys
  {:id schema/Int
   :label schema/Str})

;; TODO missing things real typing for patevyys, fields for voimassaoloaika,
;; laskentaohjelmistot and toiminta-alueet. Needs specs...

(def LaatijaSave
  "This schema is used in add-laatija and update-laatija services"
  (merge {:etunimi          schema/Str
          :sukunimi         schema/Str
          :henkilotunnus    common-schema/Hetu
          :email            schema/Str
          :puhelin          schema/Str
          :patevyys         schema/Int
          :julkinen-puhelin schema/Bool
          :julkinen-email   schema/Bool}
         common-schema/Postiosoite))

(def Laatija
  "Laatija schema contains basic information about persistent laatija"
  (assoc LaatijaSave :id schema/Num))
