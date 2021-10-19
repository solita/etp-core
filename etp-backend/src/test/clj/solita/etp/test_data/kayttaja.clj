(ns solita.etp.test-data.kayttaja
  (:require [solita.etp.test-system :as ts]
            [solita.etp.test-data.generators :as generators]
            [solita.etp.schema.kayttaja :as kayttaja-schema]
            [solita.etp.service.kayttaja :as kayttaja-service]
            [schema.core :as schema]))

(def laatija {:rooli 0})
(def patevyyden-toteaja {:rooli 1})
(def paakayttaja {:rooli 2})
(def laskuttaja {:rooli 3})
(def public nil)

(def KayttajaAdd
  "Schema to add other users for tests."
  {:etunimi  schema/Str
   :sukunimi schema/Str
   :email    schema/Str
   :puhelin  schema/Str
   :rooli    (schema/enum 1 2 3)})

(defn generate-adds [n]
  (take n (map #(generators/complete {:email %}
                                     KayttajaAdd)
               (generators/unique-emails n))))

(defn generate-updates [n]
  (take n (map #(generators/complete {:henkilotunnus %1
                                      :email %2}
                                     kayttaja-schema/KayttajaUpdate)
               (generators/unique-henkilotunnukset n)
               (generators/unique-emails n))))

(defn insert! [kayttaja-adds]
  (doall (map #(kayttaja-service/add-kayttaja! ts/*db* %) kayttaja-adds)))

(defn generate-and-insert! [n]
  (let [kayttaja-adds (generate-adds n)]
    (zipmap (insert! kayttaja-adds) kayttaja-adds)))
