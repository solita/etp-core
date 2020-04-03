(ns solita.etp.service.rooli)

(def roolit [{:id 0
              :k :laatija
              :label-fi "Laatija"
              :label-sv "Laatija-SV"}
             {:id 1
              :k :patevyyden-toteaja
              :label-fi "Pätevyyden toteaja"
              :label-sv "Pätevyyden toteaja -SV"}
             {:id 2
              :k :paakayttaja
              :label-fi "Pääkäyttäjä"
              :label-sv "Pääkäyttäjä-SV"}])

(defn find-roolit [] roolit)

(defn laatija? [{:keys [rooli]}]
  (= rooli 0))

(defn patevyydentoteaja? [{:keys [rooli]}]
  (= rooli 1))

(defn paakayttaja? [{:keys [rooli]}]
  (= rooli 2))

(defn laatija-maintainer? [{:keys [rooli]}]
  (contains? #{1 2} rooli))
