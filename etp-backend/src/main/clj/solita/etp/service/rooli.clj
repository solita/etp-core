(ns solita.etp.service.rooli
  (:require [solita.etp.service.luokittelu :as luokittelu-service]))

(def ^:private rooli-keys
  [:laatija :patevyyden-toteaja :paakayttaja])

(defn find-roolit [db] (luokittelu-service/find-roolit db))

(defn public? [whoami]
  (nil? whoami))

(defn laatija? [{:keys [rooli]}]
  (= rooli 0))

(defn patevyydentoteaja? [{:keys [rooli]}]
  (= rooli 1))

(defn paakayttaja? [{:keys [rooli]}]
  (= rooli 2))

(defn laatija-maintainer? [{:keys [rooli]}]
  (contains? #{1 2} rooli))

(defn rooli-key [rooli-id] (nth rooli-keys rooli-id))
