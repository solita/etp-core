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
