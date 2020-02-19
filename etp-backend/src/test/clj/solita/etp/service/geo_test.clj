(ns solita.etp.service.geo-test
  (:require [clojure.test :as t]
            [solita.etp.test-system :as ts]
            [solita.etp.service.geo :as service]))

(t/use-fixtures :each ts/fixture)

(t/deftest find-toimintaalueet-test
  (let [toimintaalueet (service/find-toimintaalueet)
        fi-labels (set (map :label-fi toimintaalueet))
        se-labels (set (map :label-se toimintaalueet))]
    (t/is (every? #(contains? fi-labels %) ["Pirkanmaa" "Etelä-Savo"]))
    (t/is (every? #(contains? se-labels %) ["Birkaland" "Södra Savolax"]))
    (t/is (-> fi-labels (contains? "Ahvenanmaa") not))
    (t/is (= 18 (count toimintaalueet)))))
