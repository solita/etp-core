(ns solita.etp.service.luokittelu
  (:require [clojure.java.jdbc :as jdbc]
            [solita.etp.db :as db]))

(defn- select-luokittelu [db table-name]
  (jdbc/query db
              [(format "SELECT id, label_fi, label_sv, valid FROM %s" table-name)]
              db/default-opts))

(def find-ilmanvaihtotyypit #(select-luokittelu % "ilmanvaihtotyyppi"))
(def find-lammitysmuodot #(select-luokittelu % "lammitysmuoto"))
(def find-lammonjaot #(select-luokittelu % "lammonjako"))

(defn- path= [value path object]
  (= value (get-in object path)))

(def ilmanvaihto-kuvaus-required? (partial path= 6 [:lahtotiedot :ilmanvaihto :tyyppi-id]))
(def lammitysmuoto-1-kuvaus-required? (partial path= 9, [:lahtotiedot :lammitys :lammitysmuoto-1 :id]))
(def lammitysmuoto-2-kuvaus-required? (partial path= 9, [:lahtotiedot :lammitys :lammitysmuoto-2 :id]))
(def lammonjako-kuvaus-required? (partial path= 6 [:lahtotiedot :lammitys :lammonjako :id]))