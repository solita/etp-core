(ns solita.etp.schema.yritys
  (:require [schema.core :as schema]))

(def Postiosoite
  {:katuosoite       schema/Str
   :postilokero      schema/Str
   :postinumero      schema/Str
   :postitoimipaikka schema/Str
   :maa              schema/Str})

(def YritysSave
  "This schema is used in add-yritys and update-yritys services"
  {:ytunnus      schema/Str
   :nimi         schema/Str
   :posti-osoite Postiosoite
   :www-osoite   schema/Str
   :email        schema/Str})

(def Yritys
  "Yritys schema contains basic information about persistent yritys"
  (assoc YritysSave :id schema/Num))
