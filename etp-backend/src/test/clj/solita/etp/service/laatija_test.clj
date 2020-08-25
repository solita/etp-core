(ns solita.etp.service.laatija-test
  (:require [clojure.test :as t]
            [solita.etp.test-system :as ts]
            [solita.etp.service.laatija :as service]
            [solita.etp.service.kayttaja-laatija-test :as laatija-service-test]
            [solita.etp.service.kayttaja-laatija :as laatija-service]))

(t/use-fixtures :each ts/fixture)

(t/deftest find-patevyydet-test
  (let [patevyydet (service/find-patevyystasot)
        fi-labels  (map :label-fi patevyydet)
        se-labels  (map :label-sv patevyydet)]
    (t/is (= ["Perustaso" "Ylempi taso"] fi-labels))
    (t/is (= ["Basnivå" "Högre nivå"] se-labels))))

(t/deftest find-all-laatijat-test
  (let [laatija (first (laatija-service-test/generate-KayttajaLaatijaAdds 1))]
    (#'laatija-service/upsert-kayttaja-laatija! ts/*db* laatija)

    (t/is (= 1 (count (service/find-all-laatijat ts/*db* {:rooli 0}))))
    (t/is (not (contains? (first (service/find-all-laatijat ts/*db* {:rooli 0})) :henkilotunnus)))

    (t/is (= 1 (count (service/find-all-laatijat ts/*db* {:rooli 1}))))
    (t/is (= (subs (:henkilotunnus laatija) 0 6) (-> (service/find-all-laatijat ts/*db* {:rooli 1}) first :henkilotunnus)))

    (t/is (= 1 (count (service/find-all-laatijat ts/*db* {:rooli 2}))))
    (t/is (= (:henkilotunnus laatija) (-> (service/find-all-laatijat ts/*db* {:rooli 2}) first :henkilotunnus)))))

;; TODO test for finding, attaching and detaching yritys from laatija
