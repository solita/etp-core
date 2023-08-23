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
      (t/testing "11 results in an exception"
        (t/is (thrown? Exception (aineisto/set-kayttaja-aineistot! ts/*db* 1 (take 11 test-aineisto)))))
      (t/testing "10 is allowed, return value 1 tells inserts succeeded"
        (t/is (= 1
                 (aineisto/set-kayttaja-aineistot! ts/*db* 1 (take 10 test-aineisto))))))))


(def user-id-with-allowed-ip 66666)
(def allowed-network "192.168.11.1/32")
(def actual-ip "192.168.11.1")
(def allowed-aineisto-type 1)

(t/deftest check-access-test
  ;; Set up a user and allow it to access aineisto from the given ip
  (jdbc/execute! ts/*db* ["insert into kayttaja (id, rooli_id, etunimi, sukunimi, email, puhelin) VALUES (?, 4, 'testiaineisto', 'testikäyttäjä', 'testi@solita.fi', '')", user-id-with-allowed-ip])
  (jdbc/execute! ts/*db* ["insert into kayttaja_aineisto (kayttaja_id, aineisto_id, valid_until, ip_address) VALUES (?, ?, ?, ?)"
                          user-id-with-allowed-ip
                          allowed-aineisto-type
                          (-> (Instant/now)
                              (.plusSeconds 864000))
                          allowed-network])

  (t/testing "User has access with the given ip address"
    (t/is (true? (aineisto/check-access ts/*db*, user-id-with-allowed-ip, allowed-aineisto-type, actual-ip))))

  (t/testing "User has no access to another aineistotype even if the ip is allowed"
    (t/is (false? (aineisto/check-access ts/*db*, user-id-with-allowed-ip, 2, actual-ip))))

  (t/testing "User doesn't have access with another ip than the allowed one"
    (t/is (false? (aineisto/check-access ts/*db*, user-id-with-allowed-ip, allowed-aineisto-type, "192.168.1.2"))))

  (t/testing "User can't access aineistot from an ip that is allowed for different user"
    (jdbc/execute! ts/*db* ["insert into kayttaja (id, rooli_id, etunimi, sukunimi, email, puhelin) VALUES (?, 4, 'Ei testiaineistoa', 'testikäyttäjä', 'testi2@solita.fi', '')", 666667])

    (t/is (false? (aineisto/check-access ts/*db*, 666667, allowed-aineisto-type, actual-ip))))

  (t/testing "User can't access aineistot after access has been removed"
    (aineisto/delete-kayttaja-access! ts/*db* user-id-with-allowed-ip)
    (t/is (false? (aineisto/check-access ts/*db*, user-id-with-allowed-ip, allowed-aineisto-type, actual-ip))))

  (t/testing "User can't access aineistot when the access has expired"
    ;; Add access that has valid_until in the past
    (jdbc/execute! ts/*db* ["insert into kayttaja_aineisto (kayttaja_id, aineisto_id, valid_until, ip_address) VALUES (?, ?, ?, ?)"
                            user-id-with-allowed-ip
                            allowed-aineisto-type
                            (-> (Instant/now)
                                (.minusSeconds 5))
                            allowed-network])
    (t/is (false? (aineisto/check-access ts/*db*, user-id-with-allowed-ip, allowed-aineisto-type, actual-ip))))

  (t/testing "Access can be just an ip address"
    (jdbc/execute! ts/*db* ["insert into kayttaja_aineisto (kayttaja_id, aineisto_id, valid_until, ip_address) VALUES (?, ?, ?, ?)"
                            user-id-with-allowed-ip
                            allowed-aineisto-type
                            (-> (Instant/now)
                                (.plusSeconds 86400))
                            actual-ip])
    (t/is (true? (aineisto/check-access ts/*db*, user-id-with-allowed-ip, allowed-aineisto-type, actual-ip)))))
