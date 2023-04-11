(ns solita.etp.service.aineisto-test
  (:require [solita.etp.service.aineisto :as aineisto]
            [solita.etp.test-system :as ts]
            [clojure.test :as t])
  (:import (java.time Instant)))

(t/use-fixtures :each ts/fixture)

(def test-aineisto
  (repeatedly (constantly {:aineisto-id 1,
                           :valid-until (Instant/now),
                           :ip-address  "192.168.56.22"})))

(t/deftest set-kayttaja-aineistot!-test
  (t/testing "Maximum of ten aineistot is allowed"
    ;; Mock the actual database inserts as they are not necessary for these tests
    (with-redefs [aineisto/set-access! (fn [_ _ _] true)]
      (t/testing "11 results in nil"
        (t/is (thrown? Exception (aineisto/set-kayttaja-aineistot! ts/*db* 1 (take 11 test-aineisto)))))
      (t/testing "10 is allowed, return value 1 tells inserts succeeded"
        (t/is (= 1
                 (aineisto/set-kayttaja-aineistot! ts/*db* 1 (take 10 test-aineisto))))))))
