(ns solita.etp.service.rooli)

(def roolit [{:id 0
              :label-fi "Laatija"
              :label-sv "Laatija-SV"}
             {:id 1
              :label-fi "Pätevyyden toteaja"
              :label-sv "Pätevyyden toteaja -SV"}
             {:id 2
              :label-fi "Pääkäyttäjä"
              :label-sv "Pääkäyttäjä-SV"}])

(def ^:private rooli-keys
  [:laatija :patevyyden-toteaja :paakayttaja])

(defn find-roolit [] roolit)

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
