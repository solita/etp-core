(ns solita.etp.service.energiatodistus-csv-test
  (:require [clojure.test :as t]
            [solita.common.map :as xmap]
            [solita.etp.test-system :as ts]
            [solita.etp.test-data.laatija :as laatija-test-data]
            [solita.etp.test-data.energiatodistus :as energiatodistus-test-data]
            [solita.etp.schema.energiatodistus :as schema]
            [solita.etp.service.complete-energiatodistus
             :as complete-energiatodistus-service]
            [solita.etp.service.energiatodistus-csv :as service]))

(t/use-fixtures :each ts/fixture)

(defn test-data-set []
  (let [laatijat (laatija-test-data/generate-and-insert! 1)
        energiatodistukset (energiatodistus-test-data/generate-and-insert!
                            100
                            2018
                            true
                            (-> laatijat keys sort first))]
    {:laatijat laatijat
     :energiatodistukset energiatodistukset}))

(t/deftest columns-test
  (let [luokittelut (complete-energiatodistus-service/luokittelut ts/*db*)]
    (t/is (every? vector? service/columns))
    (t/is (every? #(or (keyword? %)
                       (integer? %))
                  (apply concat service/columns)))

    ;; Tests that all paths in generated energiatodistus are
    ;; found in columns listed in the service. Basically for finding typos
    ;; in configuration.
    (t/is (every? #(contains? (set service/columns) %)
                  (->> (energiatodistus-test-data/generate-adds 100 2018 true)
                       (map #(complete-energiatodistus-service/complete-energiatodistus
                              %
                              luokittelut))
                       (map #(dissoc % :kommentti))
                       (map #(xmap/dissoc-in % [:tulokset :kuukausierittely]))
                       (map #(xmap/dissoc-in % [:tulokset
                                                :kuukausierittely-summat]))
                       (map #(xmap/dissoc-in % [:tulokset :e-luokka-rajat]))
                       (map xmap/paths)
                       (apply concat)
                       (filter #(every? keyword? %)))))))

(t/deftest column-ks->str-test
  (t/is (= "" (service/column-ks->str [])))
  (t/is (= "Foo / B / 5 / X" (service/column-ks->str
                              [:foo "b" 5 :x]))))

(t/deftest csv-line-test
  (t/is (= "\n" (service/csv-line [])))
  (t/is (= "\"test\",1,2\n" (service/csv-line
                             ["test" 1 2]))))

(t/deftest find-energiatodistukset-csv-test
  (let [{:keys [laatijat energiatodistukset]} (test-data-set)
        laatija-id (-> laatijat keys sort first)]
    (t/is (instance? java.io.InputStream
                     (service/find-energiatodistukset-csv
                      ts/*db*
                      {:id laatija-id :rooli 0}
                      {})))))
