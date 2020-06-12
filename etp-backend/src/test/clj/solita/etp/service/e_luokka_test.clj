(ns solita.etp.service.e-luokka-test
  (:require [clojure.test :as t]
            [solita.etp.test-system :as ts]
            [solita.etp.service.e-luokka :as service]))

(t/deftest limit-test
  (t/is (= 125 (service/limit 150 215 0.6)))
  (t/is (= 124 (service/limit 150.5 215 0.6))))

(t/deftest e-luokka-from-e-luku-and-nettoala-test
  (t/is (= "C" (service/e-luokka-from-e-luku-and-nettoala
                100
                100
                [[100 2 "A"] [200 2 "B"] [300 2 "C"] [400 2 "D"]]
                "E")))
  (t/is (= "B" (service/e-luokka-from-e-luku-and-nettoala
                100
                100
                [[100 2 "A"]]
                "B"))))

(t/deftest pienet-asuinrakennukset-50-150-2018
  (t/is (= "B" (service/pienet-asuinrakennukset-50-150-2018 125 150)))
  (t/is (= "C" (service/pienet-asuinrakennukset-50-150-2018 126 150))))

(t/deftest find-e-luokka-test
  (t/is (= {:e-luokka "A"} (service/find-e-luokka ts/*db* 2018 "ABC" 100 120))))
