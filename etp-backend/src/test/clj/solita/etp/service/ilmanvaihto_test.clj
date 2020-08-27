(ns solita.etp.service.ilmanvaihto-test
  (:require [clojure.test :as t]
            [solita.etp.test-system :as ts]
            [solita.etp.service.ilmanvaihtotyyppi :as service]))

(t/use-fixtures :each ts/fixture)

(t/deftest find-toimintaalueet-test
  (let [ilmanvaihtotyypit (service/find-ilmanvaihtotyypit ts/*db*)]
    (t/is (= (-> ilmanvaihtotyypit last :label-fi) "Muu, mikä"))
    (t/is (= 7 (count ilmanvaihtotyypit)))))
