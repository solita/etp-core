(ns solita.etp.service.laatija-test
  (:require [clojure.test :as t]
            [solita.etp.test-system :as ts]
            [solita.etp.service.laatija :as service]))

(t/use-fixtures :each ts/fixture)

(t/deftest find-patevyydet-test
  (let [patevyydet (service/find-patevyystasot)
        fi-labels  (map :label-fi patevyydet)
        se-labels  (map :label-sv patevyydet)]
    (t/is (= ["Perustaso" "Ylempi taso"] fi-labels))
    (t/is (= ["Basnivå" "Högre nivå"] se-labels))))

;; TODO test for finding, attaching and detaching yritys from laatija
