(ns solita.etp.service.energiatodistus-search-test
  (:require [solita.etp.service.energiatodistus-test :as energiatodistus-test]
            [solita.etp.service.energiatodistus-search :as energiatodistus-search-service]
            [solita.etp.test-system :as ts]
            [clojure.test :as t])
  (:import (clojure.lang ExceptionInfo)))

(t/use-fixtures :each ts/fixture)

(defn search [where]
  (energiatodistus-search-service/search ts/*db* {:where where}))

(t/deftest not-found-test
  (t/is (= (search [[["=" "id" -1]]]) [])))

(t/deftest add-and-find-by-id-test
  (let [laatija-id (energiatodistus-test/add-laatija!)]
    (doseq [energiatodistus (repeatedly 1 energiatodistus-test/generate-energiatodistus-2018)
            :let [id (energiatodistus-test/add-energiatodistus! energiatodistus laatija-id 2018)]]
      (t/is (= (energiatodistus-test/complete-energiatodistus energiatodistus id laatija-id 2018)
               (dissoc (first (search [[["=" "id" id]]]))
                       :laatija-fullname
                       :korvaava-energiatodistus-id))))))

(t/deftest add-and-find-by-nimi-test
  (let [laatija-id (energiatodistus-test/add-laatija!)]
    (let [energiatodistus
          (assoc-in (energiatodistus-test/generate-energiatodistus-2018)
                    [:perustiedot :nimi] "test")
          id (energiatodistus-test/add-energiatodistus! energiatodistus laatija-id 2018)]

      (t/is (= (energiatodistus-test/complete-energiatodistus energiatodistus id laatija-id 2018)
               (dissoc (first (search [[["=" "perustiedot.nimi" "test"]]]))
                       :laatija-fullname
                       :korvaava-energiatodistus-id))))))

(t/deftest add-and-find-by-nimi-and-id-test
  (let [laatija-id (energiatodistus-test/add-laatija!)]
    (let [energiatodistus
          (assoc-in (energiatodistus-test/generate-energiatodistus-2018)
                    [:perustiedot :nimi] "test")
          id (energiatodistus-test/add-energiatodistus! energiatodistus laatija-id 2018)]

      (t/is (= (energiatodistus-test/complete-energiatodistus energiatodistus id laatija-id 2018)
               (dissoc (first (search [[["=" "perustiedot.nimi" "test"]["=" "id" id]]]))
                       :laatija-fullname
                       :korvaava-energiatodistus-id))))))

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
