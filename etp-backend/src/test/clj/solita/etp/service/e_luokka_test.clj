(ns solita.etp.service.e-luokka-test
  (:require [clojure.test :as t]
            [solita.etp.test-system :as ts]
            [solita.etp.service.e-luokka :as service]))

(t/use-fixtures :each ts/fixture)

(t/deftest rajat-without-kertoimet-test
  (t/is (= [[10 "A"] [100 "B"] [550 "C"]]
           (service/rajat-without-kertoimet
            [[100 0.9 "A"] [200 1 "B"] [300 -2.5 "C"]] 100))))

(t/deftest e-luokka-from-rajat-test
  (let [rajat [[10 "A"] [20 "B"] [30 "C"] [40 "D"] [50 "E"] [60 "F"]]]
    (t/is (= "A" (service/e-luokka-from-rajat rajat 9)))
    (t/is (= "A" (service/e-luokka-from-rajat rajat 10)))
    (t/is (= "B" (service/e-luokka-from-rajat rajat 11)))
    (t/is (= "C" (service/e-luokka-from-rajat rajat 29)))
    (t/is (= "D" (service/e-luokka-from-rajat rajat 31)))
    (t/is (= "E" (service/e-luokka-from-rajat rajat 41)))
    (t/is (= "F" (service/e-luokka-from-rajat rajat 59)))
    (t/is (= "G" (service/e-luokka-from-rajat rajat 61)))))

(defn find-e-luokka-info [versio alakayttotarkoitus-id nettoala e-luku]
  (service/find-e-luokka-info ts/*db* versio alakayttotarkoitus-id nettoala e-luku))

(t/deftest find-e-luokka-info-test
  (t/is (= {:e-luokka "A"
            :luokittelu {:id 1
                         :label-fi "Erilliset pientalot"
                         :label-sv "Fristående småhus"}
            :rajat [[94 "A"] [164 "B"] [204 "C"] [284 "D"] [414 "E"] [484 "F"]]}
           (find-e-luokka-info 2013 "YAT" 100 1)))
  (t/is (= {:e-luokka "A"
            :luokittelu {:id 1
                         :label-fi "Erilliset pientalot"
                         :label-sv "Fristående småhus"}
            :rajat [[79 "A"] [125 "B"] [162 "C"] [242 "D"] [372 "E"] [442 "F"]]}
           (find-e-luokka-info 2013 "YAT" 150 1)))
  (t/is (= {:e-luokka "A"
            :luokittelu {:id 1
                         :label-fi "1. Pienet asuinrakennukset"
                         :label-sv "Små bostadsbyggnader"}
            :rajat [[90 "A"] [155 "B"] [192 "C"] [272 "D"] [402 "E"] [472 "F"]]}
           (find-e-luokka-info 2018 "KAT" 100 1)))
  (t/is (= {:e-luokka "B"
            :luokittelu {:id 1
                         :label-fi "Erilliset pientalot"
                         :label-sv "Fristående småhus"}
            :rajat [[94 "A"] [164 "B"] [204 "C"] [284 "D"] [414 "E"] [484 "F"]]}
           (find-e-luokka-info 2013 "MAEP" 100 95)))
  (t/is (= {:e-luokka "D"
            :luokittelu {:id 2
                         :label-fi "Rivi- ja ketjutalot"
                         :label-sv "Rad- och kedjehus"}
            :rajat [[80 "A"] [110 "B"] [150 "C"] [210 "D"] [340 "E"] [410 "F"]]}
           (find-e-luokka-info 2013 "RK" 100 151)))
  (t/is (= {:e-luokka "D"
            :luokittelu {:id 1
                         :label-fi "1. Pienet asuinrakennukset"
                         :label-sv "Små bostadsbyggnader"}
            :rajat [[80 "A"] [110 "B"] [150 "C"] [210 "D"] [340 "E"] [410 "F"]]}
           (find-e-luokka-info 2018 "RT" 100 151)))
  (t/is (= {:e-luokka "D"
            :luokittelu {:id 2
                         :label-fi "2. Asuinkerrostalot"
                         :label-sv "Flervåningsbostadshus"}
            :rajat [[75 "A"] [100 "B"] [130 "C"] [160 "D"] [190 "E"] [240 "F"]]}
           (find-e-luokka-info 2018 "AK3" 100 151)))
  (t/is (= {:e-luokka "E"
            :luokittelu {:id 4
                         :label-fi "Toimistorakennukset"
                         :label-sv "Kontorsbyggnader"}
            :rajat [[80 "A"] [120 "B"] [170 "C"] [200 "D"] [240 "E"] [300 "F"]]}
           (find-e-luokka-info 2013 "T" 100 240)))
  (t/is (= {:e-luokka "E"
            :luokittelu {:id 3
                         :label-fi "3. Toimistorakennukset"
                         :label-sv "Kontorsbyggnader"}
            :rajat [[80 "A"] [120 "B"] [170 "C"] [200 "D"] [240 "E"] [300 "F"]]}
           (find-e-luokka-info 2018 "TE" 100 240)))
  (t/is (= {:e-luokka "F"
            :luokittelu {:id 5
                         :label-fi "Liikerakennukset"
                         :label-sv "Affärsbyggnader"}
            :rajat  [[90 "A"] [170 "B"] [240 "C"] [280 "D"] [340 "E"] [390 "F"]]}
           (find-e-luokka-info 2013 "TOKK" 100 380)))
  (t/is (= {:e-luokka "F"
            :luokittelu {:id 4
                         :label-fi "4. Liikerakennukset"
                         :label-sv "Affärsbyggnader"}
            :rajat [[90 "A"] [170 "B"] [240 "C"] [280 "D"] [340 "E"] [390 "F"]]}
           (find-e-luokka-info 2018 "MH" 100 380)))
  (t/is (= {:e-luokka "C"
            :luokittelu {:id 5
                         :label-fi "Liikerakennukset"
                         :label-sv "Affärsbyggnader"}
            :rajat [[90 "A"] [170 "B"] [240 "C"] [280 "D"] [340 "E"] [390 "F"]]}
           (find-e-luokka-info 2013 "N" 100 240)))
  (t/is (= {:e-luokka "C"
            :luokittelu {:id 4
                         :label-fi "4. Liikerakennukset"
                         :label-sv "Affärsbyggnader"}
            :rajat [[90 "A"] [170 "B"] [240 "C"] [280 "D"] [340 "E"] [390 "F"]]}
           (find-e-luokka-info 2018 "N" 100 240)))
  (t/is (= {:e-luokka "D"
            :luokittelu {:id 7
                         :label-fi "Opetusrakennukset ja päiväkodit"
                         :label-sv "Undervisningsbyggnader och daghem"}
            :rajat [[90 "A"] [130 "B"] [170 "C"] [230 "D"] [300 "E"] [360 "F"]]}
           (find-e-luokka-info 2013 "AOR" 100 230)))
  (t/is (= {:e-luokka "D"
            :luokittelu {:id 6
                         :label-fi "6. Opetusrakennukset ja päiväkodit"
                         :label-sv "Undervisningsbyggnader och daghem"}
            :rajat [[90 "A"] [130 "B"] [170 "C"] [230 "D"] [300 "E"] [360 "F"]]}
           (find-e-luokka-info 2018 "OR" 100 230)))
  (t/is (= {:e-luokka "G"
            :luokittelu {:id 8
                         :label-fi "Liikuntahallit pois lukien uima- ja jäähallit"
                         :label-sv "Idrottshallar (med undantag av simhallar och ishallar)"}
            :rajat [[90 "A"] [130 "B"] [170 "C"] [190 "D"] [240 "E"] [280 "F"]]}
           (find-e-luokka-info 2013 "TSSJ" 100 282)))
  (t/is (= {:e-luokka "G"
            :luokittelu {:id 7
                         :label-fi "7. Liikuntahallit, lukuun ottamatta uimahalleja ja jäähalleja"
                         :label-sv "Idrottshallar, med undantag för simhallar och ishallar"}
            :rajat [[90 "A"] [130 "B"] [170 "C"] [190 "D"] [240 "E"] [280 "F"]]}
           (find-e-luokka-info 2018 "LH" 100 282)))
  (t/is (= {:e-luokka "E"
            :luokittelu {:id 9
                         :label-fi "Sairaalat"
                         :label-sv "Sjukhus"}
            :rajat [[150 "A"] [350 "B"] [450 "C"] [550 "D"] [650 "E"] [800 "F"]]}
           (find-e-luokka-info 2013 "MS" 100 650)))
  (t/is (= {:e-luokka "E"
            :luokittelu {:id 8
                         :label-fi "8. Sairaalat"
                         :label-sv "Sjukhus"}
            :rajat [[150 "A"] [350 "B"] [450 "C"] [550 "D"] [650 "E"] [800 "F"]]}
           (find-e-luokka-info 2018 "S" 100 650)))
  (t/is (= {:e-luokka "B"
            :luokittelu {:id 9
                         :label-fi "9. Muut rakennukset"
                         :label-sv "Övriga byggnader"}
            :rajat [[90 "A"] [130 "B"] [170 "C"] [190 "D"] [240 "E"] [280 "F"]]}
           (find-e-luokka-info 2018 "JH" 100 130)))
  (t/is (nil? (find-e-luokka-info 2018 "NONEXISTING" 100 130)))
  (t/is (nil? (find-e-luokka-info 2080 "MAEP" 100 130))) )
