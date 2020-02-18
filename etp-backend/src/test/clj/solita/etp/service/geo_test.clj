(ns solita.etp.service.geo-test
  (:require [clojure.test :as t]
            [solita.etp.test-system :as ts]
            [solita.etp.service.geo :as service]))

(t/use-fixtures :each ts/fixture)

(t/deftest find-toimintaalueet-test
  (let [toimintaalueet (->> (service/find-toimintaalueet) (map :label) set)]
    (t/is (every? #(contains? toimintaalueet %) ["Pirkanmaa" "Etelä-Savo"]))
    (t/is (-> toimintaalueet (contains? "Ahvenanmaa") not))
    (t/is (= 18 (count toimintaalueet)))))
