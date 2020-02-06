(ns solita.etp.example-test
  (:require [clojure.test :as t]
            [clojure.java.jdbc :as jdbc]
            [solita.etp.test-system :as test-system]))

;; These tests should be removed when we have real tests
;; For now these are used to verify that tests running
;; in parallel have separate systems (separate databases)



(defn create-laatija! [db laatija]
  (jdbc/insert! db :etp.laatija laatija))

(defn get-laatija-count [db]
  (-> db (jdbc/query ["SELECT COUNT(id) FROM etp.laatija"]) first :count))

(t/deftest example-1
  (let [{:keys [db] :as systems} (test-system/start!)]
    (t/is (zero? (get-laatija-count db)))
    (create-laatija! db {:kayttajatunnus "dcooper"
                        :hetu "010187-111A"
                        :etunimi "Dale"
                        :sukunimi "Cooper"})
    (Thread/sleep 1000)
    (t/is (= 1 (get-laatija-count db)))
    (Thread/sleep 2000)
    (test-system/stop! systems)))

(t/deftest example-1
  (let [{:keys [db] :as systems} (test-system/start!)]
    (t/is (zero? (get-laatija-count db)))
    (create-laatija! db {:kayttajatunnus "wearle"
                         :hetu "020245-222B"
                         :etunimi "Windom"
                         :sukunimi "Earle"})
    (Thread/sleep 1000)
    (t/is (= 1 (get-laatija-count db)))
    (Thread/sleep 2000)
    (test-system/stop! systems)))
