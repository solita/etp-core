(ns solita.etp.example-test
  (:require [clojure.test :as t]
            [clojure.java.jdbc :as jdbc]
            [solita.etp.test-system :as ts]))

;; These tests should be removed when we have real tests
;; For now these are used to verify that tests running
;; in parallel have separate systems (separate databases)

(t/use-fixtures :each ts/fixture)

(defn create-laatija! [db laatija]
  (jdbc/insert! db :etp.laatija laatija))

(defn get-laatija-count [db]
  (-> db (jdbc/query ["SELECT COUNT(id) FROM laatija"]) first :count))

(t/deftest example-1
  (t/is (zero? (get-laatija-count ts/*db*)))
  (create-laatija! ts/*db* {:kayttajatunnus "dcooper"
                            :hetu "010187-111A"
                            :etunimi "Dale"
                            :sukunimi "Cooper"})
  (Thread/sleep 1000)
  (t/is (= 1 (get-laatija-count ts/*db*)))
  (Thread/sleep 2000))

(t/deftest example-1
  (t/is (zero? (get-laatija-count ts/*db*)))
  (create-laatija! ts/*db* {:kayttajatunnus "wearle"
                            :hetu "020245-222B"
                            :etunimi "Windom"
                            :sukunimi "Earle"})
  (Thread/sleep 1000)
  (t/is (= 1 (get-laatija-count ts/*db*)))
  (Thread/sleep 2000))
