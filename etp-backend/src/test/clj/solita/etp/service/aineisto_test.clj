(ns solita.etp.service.aineisto-test
  (:require [solita.etp.service.aineisto :as aineisto]
            [solita.etp.test-system :as ts]
            [clojure.test :as t]
            [clojure.java.jdbc :as jdbc])
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


(def user-id-with-allowed-ip 66666)
(def allowed-ip "192.168.11.1/32")

(t/deftest check-access-test
  (t/testing "User has access with the given ip-address"

    (jdbc/execute! ts/*db* ["insert into kayttaja (id, rooli_id, etunimi, sukunimi, email, puhelin) VALUES (?, 4, 'testiaineisto', 'testikäyttäjä', 'testi@solita.fi', '')", user-id-with-allowed-ip])
    (jdbc/execute! ts/*db* ["insert into kayttaja_aineisto (kayttaja_id, aineisto_id, valid_until, ip_address) VALUES (?, 1, ?, ?)"
                            user-id-with-allowed-ip
                            (-> (Instant/now)
                                (.plusSeconds 864000))
                            allowed-ip])
    (t/is (true? (aineisto/check-access ts/*db*, user-id-with-allowed-ip, 1, allowed-ip)))))