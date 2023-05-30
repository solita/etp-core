(ns solita.etp.service.valvonta-kaytto.toimenpide-test
  (:require [clojure.test :as t]
            [solita.etp.service.valvonta-kaytto.toimenpide :as toimenpide]))

(t/deftest type-key-test
  (t/testing "type-id 0 returns the type-key :case"
    (t/is (= (toimenpide/type-key 0)
             :case)))
  (t/testing "type-id 1 return the type-key :rfi-request"
    (t/is (= (toimenpide/type-key 1)
             :rfi-request)))

  (t/testing "type-id 2 return the type-key :rfi-order"
    (t/is (= (toimenpide/type-key 2)
             :rfi-order)))

  (t/testing "type-id 3 return the type-key :rfi-warning"
    (t/is (= (toimenpide/type-key 3)
             :rfi-warning)))

  (t/testing "type-id 4 return the type-key :decision-order"
    (t/is (= (toimenpide/type-key 4)
             :decision-order)))

  (t/testing "type-id 5 return the type-key :closed"
    (t/is (= (toimenpide/type-key 5)
             :closed))))