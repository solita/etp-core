(ns solita.etp.example-test
  (:require [clojure.test :as t]
            [clojure.java.jdbc :as jdbc]
            [solita.etp.test-system :as test-system]))

(defn create-laatija! [db laatija]
  (jdbc/insert! db :etp.laatija laatija))

(defn get-laatija-count [db]
  (-> db (jdbc/query ["SELECT COUNT(id) FROM etp.laatija"]) first :count))

;; These tests should be removed when we have real tests
;; For now these are used to verify that tests running
;; in parallel have separate systems (separate databases)

(t/deftest example-1
  (let [{:keys [db] :as systems} (test-system/start!)]
    (t/is (zero? (get-laatija-count db)))
    (create-laatija! db {:kayttajatunnus "dcooper"
                        :hetu "010187-111A"
                        :etunimi "Dale"
                        :sukunimi "Cooper"})
    (t/is (= 1 (get-laatija-count db)))
    (test-system/stop! systems)))

(t/deftest example-1
  (let [{:keys [db] :as systems} (test-system/start!)]
    (t/is (zero? (get-laatija-count db)))
    (create-laatija! db {:kayttajatunnus "wearle"
                         :hetu "020245-222B"
                         :etunimi "Windom"
                         :sukunimi "Earle"})
    (t/is (= 1 (get-laatija-count db)))
    (test-system/stop! systems)))
