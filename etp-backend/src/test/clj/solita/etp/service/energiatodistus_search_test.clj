(ns solita.etp.service.energiatodistus-search-test
  (:require [solita.etp.service.energiatodistus-test :as energiatodistus-test]
            [solita.etp.service.energiatodistus-search :as energiatodistus-search-service]
            [solita.etp.test-system :as ts]
            [clojure.test :as t])
  (:import (clojure.lang ExceptionInfo)
           (java.time LocalDate)))

(t/use-fixtures :each ts/fixture)

(defn search [where]
  (map #(dissoc % :laatija-fullname)
       (energiatodistus-search-service/search ts/*db* {:where where})))

(t/deftest not-found-test
  (t/is (= (search [[["=" "id" -1]]]) [])))

(t/deftest add-and-find-by-id-test
  (let [laatija-id (energiatodistus-test/add-laatija!)]
    (doseq [energiatodistus (repeatedly 1 energiatodistus-test/generate-energiatodistus-2018)
            :let [id (energiatodistus-test/add-energiatodistus! energiatodistus laatija-id 2018)]]
      (t/is (= (energiatodistus-test/energiatodistus-with-db-fields energiatodistus id laatija-id 2018)
               (first (search [[["=" "id" id]]])))))))

(t/deftest add-and-find-by-nimi-test
  (let [laatija-id (energiatodistus-test/add-laatija!)]
    (let [energiatodistus
          (assoc-in (energiatodistus-test/generate-energiatodistus-2018)
                    [:perustiedot :nimi] "test")
          id (energiatodistus-test/add-energiatodistus! energiatodistus laatija-id 2018)]

      (t/is (= (energiatodistus-test/energiatodistus-with-db-fields energiatodistus id laatija-id 2018)
               (first (search [[["=" "perustiedot.nimi" "test"]]])))))))

(t/deftest add-and-find-by-nimi-nil-test
  (let [laatija-id (energiatodistus-test/add-laatija!)]
    (let [energiatodistus
          (assoc-in (energiatodistus-test/generate-energiatodistus-2018)
                    [:perustiedot :nimi] nil)
          id (energiatodistus-test/add-energiatodistus! energiatodistus laatija-id 2018)]

      (t/is (= (energiatodistus-test/energiatodistus-with-db-fields energiatodistus id laatija-id 2018)
               (first (search [[["nil?" "perustiedot.nimi"]]])))))))

(t/deftest add-and-find-by-havainnointikaynti-test
  (let [laatija-id (energiatodistus-test/add-laatija!)]
    (let [^LocalDate date (LocalDate/now)
          energiatodistus
          (assoc-in (energiatodistus-test/generate-energiatodistus-2018)
                    [:perustiedot :havainnointikaynti] date)
          id (energiatodistus-test/add-energiatodistus! energiatodistus laatija-id 2018)]

      (t/is (= (energiatodistus-test/energiatodistus-with-db-fields energiatodistus id laatija-id 2018)
               (first (search [[["=" "perustiedot.havainnointikaynti" (.toString date)]]])))))))

(t/deftest add-and-find-by-nimi-and-id-test
  (let [laatija-id (energiatodistus-test/add-laatija!)]
    (let [energiatodistus
          (assoc-in (energiatodistus-test/generate-energiatodistus-2018)
                    [:perustiedot :nimi] "test")
          id (energiatodistus-test/add-energiatodistus! energiatodistus laatija-id 2018)]

      (t/is (= (energiatodistus-test/energiatodistus-with-db-fields energiatodistus id laatija-id 2018)
               (first (search [[["=" "perustiedot.nimi" "test"]["=" "id" id]]])))))))

(defn catch-ex-data [f]
  (try (f) (catch ExceptionInfo e (ex-data e))))

(t/deftest invalid-search-expression
  (t/is (= (:type (catch-ex-data #(search [[[1]]]))) :schema.core/error))
  (t/is (= (:type (catch-ex-data #(search [[[]]]))) :schema.core/error))

  (t/is (= (catch-ex-data #(search [[["="]]]))
           {:type :invalid-arguments, :predicate "=", :message "Wrong number of arguments: () for predicate: ="}))
  (t/is (= (catch-ex-data #(search [[["=" "id"]]]))
           {:type :invalid-arguments, :predicate "=", :message "Wrong number of arguments: (\"id\") for predicate: ="}))
  (t/is (= (catch-ex-data #(search [[["asdf" "id" 1]]]))
           {:type :unknown-predicate, :predicate "asdf", :message "Unknown predicate: asdf"}))
  (t/is (= (catch-ex-data #(search [[["=" "asdf" "test"]]]))
           {:type :unknown-field, :field "asdf", :message "Unknown field: asdf"})))
