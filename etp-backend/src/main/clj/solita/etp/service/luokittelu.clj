(ns solita.etp.service.luokittelu
  (:require [clojure.java.jdbc :as jdbc]
            [solita.etp.db :as db]))

(defn- select-luokittelu [db table-name]
  (jdbc/query db
              [(format "SELECT id, label_fi, label_sv, valid FROM %s ORDER BY ordinal ASC" table-name)]
              db/default-opts))

(def find-roolit #(select-luokittelu % "rooli"))
(def find-patevyystasot #(select-luokittelu % "patevyystaso"))

(def find-kielisyys #(select-luokittelu % "kielisyys"))
(def find-laatimisvaiheet #(select-luokittelu % "laatimisvaihe"))
(def find-ilmanvaihtotyypit #(select-luokittelu % "ilmanvaihtotyyppi"))
(def find-lammitysmuodot #(select-luokittelu % "lammitysmuoto"))
(def find-lammonjaot #(select-luokittelu % "lammonjako"))

(def find-vastaanottajaryhmat #(select-luokittelu % "vastaanottajaryhma"))

(def find-toimenpidetypes #(select-luokittelu % "vo_toimenpidetype"))
(def find-severities #(select-luokittelu % "vo_toimenpidetype"))

(defn- path= [value path object]
  (= value (get-in object path)))

(def ilmanvaihto-kuvaus-required? (partial path= 6 [:lahtotiedot :ilmanvaihto :tyyppi-id]))
(def lammitysmuoto-1-kuvaus-required? (partial path= 9, [:lahtotiedot :lammitys :lammitysmuoto-1 :id]))
(def lammitysmuoto-2-kuvaus-required? (partial path= 9, [:lahtotiedot :lammitys :lammitysmuoto-2 :id]))
(def lammonjako-kuvaus-required? (partial path= 12 [:lahtotiedot :lammitys :lammonjako :id]))

(defn find-luokka [id luokat]
  (->> luokat
       (filter (comp (partial = id) :id))
       first))
