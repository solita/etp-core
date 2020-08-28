(ns solita.etp.service.lammitys-test
  (:require [clojure.test :as t]
            [solita.etp.test-system :as ts]
            [solita.etp.service.lammitys :as service]))

(t/use-fixtures :each ts/fixture)

(t/deftest find-lammitysmuodot-test
  (let [lammitysmuodot (service/find-lammitysmuodot ts/*db*)]
    (t/is (= (-> lammitysmuodot last :label-fi) "Muu, mikä"))
    (t/is (= 10 (count lammitysmuodot)))))

(t/deftest find-lammonjaot-test
  (let [lammonjaot (service/find-lammonjaot ts/*db*)]
    (t/is (= (-> lammonjaot last :label-fi) "Muu, mikä"))
    (t/is (= 13 (count lammonjaot)))))
