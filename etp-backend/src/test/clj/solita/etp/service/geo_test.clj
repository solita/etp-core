(ns solita.etp.service.geo-test
  (:require [clojure.test :as t]
            [solita.etp.test-system :as ts]
            [solita.etp.service.geo :as service]))

(t/use-fixtures :each ts/fixture)

(t/deftest find-maakunnat-test
  (let [maakunnat (->> (service/find-maakunnat) (map :label) set)]
    (t/is (every? #(contains? maakunnat %) ["Ahvenanmaa"
                                            "Pirkanmaa"
                                            "Etelä-Savo"]))
    (t/is (= 19 (count maakunnat)))))
