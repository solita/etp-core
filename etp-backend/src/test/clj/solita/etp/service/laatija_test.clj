(ns solita.etp.service.laatija-test
  (:require [clojure.test :as t]
            [clojure.string :as str]
            [solita.etp.test-system :as ts]
            [solita.etp.service.laatija :as service]
            [solita.etp.service.kayttaja-laatija-test :as kl-service-test]
            [solita.etp.service.kayttaja-laatija :as kl-service]))

(t/use-fixtures :each ts/fixture)

(t/deftest public-laatija-test
  (let [laatija (-> (kl-service-test/generate-KayttajaLaatijaAdds 1)
                    first
                    (merge {:voimassa true
                            :laatimiskielto false
                            :julkinenpuhelin false
                            :julkinenemail true
                            :julkinenosoite false}))]
    (t/is (every? #(contains? (service/public-laatija laatija) %)
                  [:email :etunimi]))
    (t/is (not-every? #(contains? (service/public-laatija laatija) %)
                      [:puhelin :jakeluosoite :postinumero
                       :postitoimipaikka :maa]))
    (t/is (nil? (-> laatija
                    (assoc :laatimiskielto true)
                    service/public-laatija)))
    (t/is (nil? (-> laatija
                    (dissoc :voimassa)
                    service/public-laatija)))))

(t/deftest find-all-laatijat-test
  (let [paakayttaja {:rooli 2}
        patevyydentoteaja {:rooli 1}
        public nil
        laatijat (kl-service-test/generate-KayttajaLaatijaAdds 100)]
    (doseq [laatija laatijat]
      (let [id (#'kl-service/upsert-kayttaja-laatija! ts/*db* laatija)]
        (service/update-laatija-by-id! ts/*db* id {:julkinenemail true
                                                   :julkinenpuhelin false
                                                   :julkinenwwwosoite true
                                                   :julkinenosoite false})))
    (doseq [whoami [paakayttaja patevyydentoteaja public]]
      (let [found-laatijat (service/find-all-laatijat ts/*db* whoami)]
        (t/is (every? #(not (nil? %)) found-laatijat))
        (t/is (= (set (map :sukunimi laatijat))
                 (set (map :sukunimi found-laatijat))))
        (t/is (every? #(-> % :aktiivinen nil? not) found-laatijat))
        (when (= whoami paakayttaja)
          (t/is (every? #(-> % :henkilotunnus count (= 11)) found-laatijat))
          (t/is (every? #(contains? % :postitoimipaikka) found-laatijat))
          (t/is (= (set (map :henkilotunnus laatijat))
                   (set (map :henkilotunnus found-laatijat)))))
        (when (= whoami patevyydentoteaja)
          (t/is (every? #(-> % :henkilotunnus count (= 6)) found-laatijat))
          (t/is (every? #(-> % :henkilotunnus (str/includes? "-") not)
                        found-laatijat))
          (t/is (every? #(contains? % :postitoimipaikka) found-laatijat)))
        (when (= whoami public)
          (t/is (every? #(-> % (contains? :henkilotunnus) not) found-laatijat))
          (t/is (every? #(-> % (contains? :puhelin) not) found-laatijat))
          (t/is (every? #(-> % (contains? :jakeluosoite) not) found-laatijat))
          (t/is (every? #(-> % (contains? :postinumero) not) found-laatijat))
          (t/is (every? #(-> % (contains? :postitoimipaikka) not) found-laatijat))
          (t/is (every? #(-> % (contains? :maa) not) found-laatijat))
          (t/is (= (set (map :email laatijat))
                   (set (map :email found-laatijat))))
          (t/is (= (set (map :wwwosoite laatijat))
                   (set (map :wwwosoite found-laatijat)))))))))

;; TODO test for finding, attaching and detaching yritys from laatija

(t/deftest find-patevyydet-test
  (let [patevyydet (service/find-patevyystasot)
        fi-labels  (map :label-fi patevyydet)
        se-labels  (map :label-sv patevyydet)]
    (t/is (= ["Perustaso" "Ylempi taso"] fi-labels))
    (t/is (= ["Basnivå" "Högre nivå"] se-labels))))
