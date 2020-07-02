(ns solita.etp.schema.e-luokka
  (:require [schema.core :as schema]))

(def ELuokka {:e-luokka schema/Str
              :limits [schema/Any]})
