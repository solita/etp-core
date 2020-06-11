(ns solita.etp.service.e-luokka-test
  (:require [clojure.test :as t]
            [solita.etp.test-system :as ts]
            [solita.etp.service.e-luokka :as service]))

(t/deftest find-e-luokka-test
  (t/is (= {:e-luokka "A"} (service/find-e-luokka ts/*db* 2018 "ABC" 100 120))))
