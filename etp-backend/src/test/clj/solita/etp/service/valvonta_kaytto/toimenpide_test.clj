(ns solita.etp.service.valvonta-kaytto.toimenpide-test
  (:require [clojure.test :as t]
            [solita.etp.service.valvonta-kaytto.toimenpide :as toimenpide]
            [solita.etp.test-system :as ts]))

(t/use-fixtures :each ts/fixture)

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
             :closed)))

  (t/testing "type-id 7 return the type-key :decision-order-hearing-letter"
    (t/is (= (toimenpide/type-key 7)
             :decision-order-hearing-letter)))

  (t/testing "unknown type-id results in an exception"
    (t/is (thrown? Exception (toimenpide/type-key 666)))))

(t/deftest manually-deliverable?-test
  (t/testing "Toimenpide-type 7 document is sent manually"
    (t/is (true? (toimenpide/manually-deliverable? ts/*db* 7))))

  (t/testing "Toimenpide-types 0 - 6 are not sent manually"
    (doseq [toimenpide-type (range 7)]
      (t/is (false? (toimenpide/manually-deliverable? ts/*db* toimenpide-type))))))

(t/deftest kaskypaatos-toimenpide?-test
  (t/testing "Käskypäätös / kuulemiskirje is recognized as kaskypäätös-toimenpide?"
    (t/is (true? (toimenpide/kaskypaatos-toimenpide? {:type-id 7}))))

  (t/testing "Käskypäätös / varsinainen päätös is recognized as kaskypäätös-toimenpide?"
    (t/is (true? (toimenpide/kaskypaatos-toimenpide? {:type-id 8}))))

  (t/testing "Toimepidetypes with id 0-6 are not recognized as kaskypäätös-toimenpide?"
    (doseq [toimenpide-type (range 7)]
      (t/is (false? (toimenpide/kaskypaatos-toimenpide? {:type-id toimenpide-type}))))))

(t/deftest sakkopaatos-toimenpide?-test
  (t/testing "Sakko-päätös / kuulemiskirje is recognized as sakkopäätös-toimenpide?"
    (t/is (true? (toimenpide/sakkopaatos-toimenpide? {:type-id 14}))))
  (t/testing "Käskypäätös / kuulemiskirje is not recognized as sakkopäätös-toimenpide?"
    (t/is (false? (toimenpide/sakkopaatos-toimenpide? {:type-id 7})))))
