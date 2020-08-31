(ns solita.etp.service.luokittelu
  (:require [clojure.java.jdbc :as jdbc]
            [solita.etp.db :as db]))

(defn- select-luokittelu [db table-name]
  (jdbc/query db [(format "SELECT id, label_fi \"label-fi\", label_sv \"label-sv\", valid FROM %s" table-name)]))

(def find-ilmanvaihtotyypit #(select-luokittelu % "ilmanvaihtotyyppi"))
(def find-lammitysmuodot #(select-luokittelu % "lammitysmuoto"))
(def find-lammonjaot #(select-luokittelu % "lammonjako"))
