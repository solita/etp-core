(ns solita.etp.service.energiatodistus-search-test
  (:require [solita.etp.service.energiatodistus-test :as energiatodistus-test]
            [solita.etp.service.energiatodistus-search :as energiatodistus-search-service]
            [solita.etp.test-system :as ts]
            [clojure.test :as t]))

(t/use-fixtures :each ts/fixture)

(t/deftest add-and-find-by-id-test
  (let [laatija-id (energiatodistus-test/add-laatija!)]
    (doseq [energiatodistus (repeatedly 1 energiatodistus-test/generate-energiatodistus-2018)
            :let [id (energiatodistus-test/add-energiatodistus! energiatodistus laatija-id 2018)]]
      (t/is (= (energiatodistus-test/complete-energiatodistus energiatodistus id laatija-id 2018)
               (dissoc (first (energiatodistus-search-service/search ts/*db* {:where [[["=" "id" id]]]}))
                       :laatija-fullname))))))

(t/deftest add-and-find-by-nimi-test
  (let [laatija-id (energiatodistus-test/add-laatija!)]
    (let [energiatodistus
          (assoc-in (energiatodistus-test/generate-energiatodistus-2018)
                    [:perustiedot :nimi] "test")
          id (energiatodistus-test/add-energiatodistus! energiatodistus laatija-id 2018)]

      (t/is (= (energiatodistus-test/complete-energiatodistus energiatodistus id laatija-id 2018)
               (dissoc (first (energiatodistus-search-service/search
                                ts/*db* {:where [[["=" "perustiedot.nimi" "test"]]]}))
                       :laatija-fullname))))))
