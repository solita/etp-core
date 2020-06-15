(ns solita.etp.service.e-luokka-test
  (:require [clojure.test :as t]
            [solita.etp.test-system :as ts]
            [solita.etp.service.e-luokka :as service]))

(t/deftest e-luokka-from-e-luku-and-nettoala-test
  (let [limits [[100 2 "A"] [200 2 "B"] [300 2 "C"] [400 2 "D"]]]
    (t/is (= "C" (service/e-luokka-from-e-luku-and-nettoala 100
                                                            100
                                                            limits
                                                            "G")))
    (t/is (= "G" (service/e-luokka-from-e-luku-and-nettoala 10000
                                                            100
                                                            limits
                                                            "G")))))

(t/deftest e-luokka-from-e-luku-test
  (let [limits [[100 "A"] [200 "B"] [300 "C"]]]
    (t/is (= "A" (service/e-luokka-from-e-luku 100 limits "D")))
    (t/is (= "C" (service/e-luokka-from-e-luku 300 limits "D")))
    (t/is (= "D" (service/e-luokka-from-e-luku 301 limits "D")))))

(t/deftest pienet-asuinrakennukset-50-150-2018-test
  (t/is (= "B" (service/pienet-asuinrakennukset-50-150-2018 125 150)))
  (t/is (= "C" (service/pienet-asuinrakennukset-50-150-2018 126 150))))

(t/deftest pienet-asuinrakennukset-150-600-2018-test
  (t/is (= "F" (service/pienet-asuinrakennukset-150-600-2018 411 600)))
  (t/is (= "G" (service/pienet-asuinrakennukset-150-600-2018 412 600))))

(t/deftest pienet-asuinrakennukset-600-2018-test
  (t/is (= "A" (service/pienet-asuinrakennukset-600-2018 70 800)))
  (t/is (= "B" (service/pienet-asuinrakennukset-600-2018 106 800)))
  (t/is (= "C" (service/pienet-asuinrakennukset-600-2018 130 800)))
  (t/is (= "D" (service/pienet-asuinrakennukset-600-2018 210 800)))
  (t/is (= "E" (service/pienet-asuinrakennukset-600-2018 340 800)))
  (t/is (= "F" (service/pienet-asuinrakennukset-600-2018 410 800)))
  (t/is (= "G" (service/pienet-asuinrakennukset-600-2018 1000 800))))

(t/deftest find-e-luokka-test
  (t/is (= {:e-luokka "A"} (service/find-e-luokka ts/*db* 2018 "ABC" 100 120))))
