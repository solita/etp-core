(ns solita.etp.service.rooli-test
  (:require [clojure.test :as t]
            [solita.etp.service.rooli :as service]))

(t/deftest find-roolit-test
  (let [roolit (service/find-roolit)
        fi-labels (set (map :label-fi roolit))]
    (t/is (= fi-labels #{"Laatija"
                         "Pätevyyden toteaja"
                         "Pääkäyttäjä"}))
    ;; TODO test swedish labels when they exist
    ))

(t/deftest laatija?-test
  (t/is (false? (service/laatija? nil?)))
  (t/is (true? (service/laatija? {:rooli 0})))
  (t/is (false? (service/laatija? {:rooli 1})))
  (t/is (false? (service/laatija? {:rooli 2}))))

(t/deftest patevyydentoteaja?-test
  (t/is (false? (service/patevyydentoteaja? nil?)))
  (t/is (false? (service/patevyydentoteaja? {:rooli 0})))
  (t/is (true? (service/patevyydentoteaja? {:rooli 1})))
  (t/is (false? (service/patevyydentoteaja? {:rooli 2}))))

(t/deftest paakayttaja?-test
  (t/is (false? (service/paakayttaja? nil?)))
  (t/is (false? (service/paakayttaja? {:rooli 0})))
  (t/is (false? (service/paakayttaja? {:rooli 1})))
  (t/is (true? (service/paakayttaja? {:rooli 2}))))

(t/deftest laatija-maintainer?-test
  (t/is (false? (service/laatija-maintainer? nil?)))
  (t/is (false? (service/laatija-maintainer? {:rooli 0})))
  (t/is (true? (service/laatija-maintainer? {:rooli 1})))
  (t/is (true? (service/laatija-maintainer? {:rooli 2}))))
