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
  (t/is (true? (service/laatija? {:role 0})))
  (t/is (false? (service/laatija? {:role 1})))
  (t/is (false? (service/laatija? {:role 2}))))

(t/deftest patevyydentoteaja?-test
  (t/is (false? (service/patevyydentoteaja? nil?)))
  (t/is (false? (service/patevyydentoteaja? {:role 0})))
  (t/is (true? (service/patevyydentoteaja? {:role 1})))
  (t/is (false? (service/patevyydentoteaja? {:role 2}))))

(t/deftest paakayttaja?-test
  (t/is (false? (service/paakayttaja? nil?)))
  (t/is (false? (service/paakayttaja? {:role 0})))
  (t/is (false? (service/paakayttaja? {:role 1})))
  (t/is (true? (service/paakayttaja? {:role 2}))))

(t/deftest more-than-laatija?-test
  (t/is (false? (service/more-than-laatija? nil?)))
  (t/is (false? (service/more-than-laatija? {:role 0})))
  (t/is (true? (service/more-than-laatija? {:role 1})))
  (t/is (true? (service/more-than-laatija? {:role 2}))))
