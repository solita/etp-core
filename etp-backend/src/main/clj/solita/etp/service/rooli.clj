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

(defn find-roolit [] roolit)

(defn laatija? [{:keys [role]}]
  (= role 0))

(defn patevyydentoteaja? [{:keys [role]}]
  (= role 1))

(defn paakayttaja? [{:keys [role]}]
  (= role 2))
