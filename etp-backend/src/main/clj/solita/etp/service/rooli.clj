(ns solita.etp.service.rooli
  (:require [solita.etp.service.luokittelu :as luokittelu-service]))

(def ^:private rooli-keys
  [:laatija :patevyyden-toteaja :paakayttaja :laskuttaja])

(defn find-roolit [db] (luokittelu-service/find-roolit db))

(defn public? [whoami]
  (nil? whoami))

(defn- partner? [{:keys [ispartner]}]
  ispartner)

(defn laatija? [{:keys [rooli]}]
  (= rooli 0))

(def non-partner-laatija? (every-pred laatija? (comp not partner?)))

(defn patevyydentoteaja? [{:keys [rooli]}]
  (= rooli 1))

(defn paakayttaja? [{:keys [rooli]}]
  (= rooli 2))

(defn laskuttaja? [{:keys [rooli]}]
  (= rooli 3))

(defn rooli-key [rooli-id] (nth rooli-keys rooli-id))

(def energiatodistus-reader? (some-fn laatija? paakayttaja? laskuttaja?))

(def system
  {:communication -3
   :laskutus -2})
