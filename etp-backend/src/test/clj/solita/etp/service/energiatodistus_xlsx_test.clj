(ns solita.etp.service.energiatodistus-xlsx-test
  (:require [clojure.test :as t]
            [schema-generators.generators :as g]
            [solita.etp.test-system :as ts]
            [solita.etp.schema.energiatodistus :as schema]
            [solita.etp.service.energiatodistus-xlsx :as service]
            [solita.etp.service.energiatodistus :as energiatodistus-service]
            [solita.etp.service.energiatodistus-test :as energiatodistus-test]))

(t/use-fixtures :each ts/fixture)

(def energiatodistukset (repeatedly 100 energiatodistus-test/generate-energiatodistus))

(t/deftest other-paths-test
  (t/is (empty? (service/other-paths nil)))
  (t/is (empty? (service/other-paths [])))
  (t/is (= (service/other-paths (map #(assoc % :new-key 1) energiatodistukset))
           #{[:new-key]})))

(t/deftest paths-for-k-test
  (let [perustiedot-paths (service/paths-for-k energiatodistukset :perustiedot)]
    (t/is (empty? (service/paths-for-k [] :perustiedot)))
    (t/is (empty? (service/paths-for-k energiatodistukset :non-existing-k)))
    (t/is (= (first perustiedot-paths) [:perustiedot :havainnointikaynti]))
    (t/is (contains? perustiedot-paths [:perustiedot :tilaaja]))
    (t/is (-> perustiedot-paths
              (contains? [:lahtotiedot :lammitetty-nettoala])
              not))))

(t/deftest path->str-test
  (t/is (= (service/path->str []) ""))
  (t/is (= (service/path->str [:a :b :c]) "A / B / C"))
  (t/is (= (service/path->str [1 2 3]) "1 / 2 / 3"))
  (t/is (= (service/path->str [:foo "bar" 3 :baz]) "Foo / Bar / 3 / Baz")))

(t/deftest find-laatija-energiatodistukset-xlsx-test
  (let [laatija-id (energiatodistus-test/add-laatija!)]
    (doseq [energiatodistus energiatodistukset]
      (energiatodistus-test/add-energiatodistus! energiatodistus laatija-id))
    (t/is (instance? java.io.InputStream
                     (service/find-laatija-energiatodistukset-xlsx
                      ts/*db*
                      laatija-id)))))
