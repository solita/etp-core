(ns solita.etp.service.energiatodistus-search-test
  (:require [solita.etp.service.energiatodistus-test :as energiatodistus-test]
            [solita.etp.service.energiatodistus-search :as energiatodistus-search-service]
            [solita.etp.test-system :as ts]
            [clojure.test :as t])
  (:import (clojure.lang ExceptionInfo)
           (java.time LocalDate)))

(t/use-fixtures :each ts/fixture)

(defn add-laatija! []
  {:id (energiatodistus-test/add-laatija!)
   :rooli 0})

(def paakayttaja {:rooli 2})

(defn search [whoami where]
  (map #(dissoc % :laatija-fullname)
       (energiatodistus-search-service/search ts/*db* whoami {:where where})))

(t/deftest not-found-test
  (let [whoami (add-laatija!)]
    (-> (energiatodistus-test/generate-energiatodistus-2018)
        (energiatodistus-test/add-energiatodistus! (:id whoami) 2018))
    (t/is (= (search whoami [[["=" "id" -1]]]) []))))

(t/deftest add-and-find-by-id-test
  (let [whoami (add-laatija!)
        energiatodistus (energiatodistus-test/generate-energiatodistus-2018)
        id (energiatodistus-test/add-energiatodistus! energiatodistus
                                                      (:id whoami)
                                                      2018)]
    (t/is (= (energiatodistus-test/energiatodistus-with-db-fields
              energiatodistus
              id
              (:id whoami)
              2018)
             (first (search whoami [[["=" "id" id]]]))))))

(t/deftest add-and-find-by-nimi-test
  (let [whoami (add-laatija!)
        energiatodistus (-> (energiatodistus-test/generate-energiatodistus-2018)
                            (assoc-in [:perustiedot :nimi] "test"))
        id (energiatodistus-test/add-energiatodistus! energiatodistus
                                                      (:id whoami)
                                                      2018)]
    (t/is (= (energiatodistus-test/energiatodistus-with-db-fields
              energiatodistus
              id
              (:id whoami)
              2018)
             (first (search whoami [[["=" "perustiedot.nimi" "test"]]]))))))

(t/deftest add-and-find-by-nimi-nil-test
  (let [whoami (add-laatija!)
        energiatodistus (-> (energiatodistus-test/generate-energiatodistus-2018)
                            (assoc-in [:perustiedot :nimi] nil))
        id (energiatodistus-test/add-energiatodistus! energiatodistus
                                                      (:id whoami)
                                                      2018)]
    (t/is (= (energiatodistus-test/energiatodistus-with-db-fields
              energiatodistus
              id
              (:id whoami)
              2018)
             (first (search whoami [[["nil?" "perustiedot.nimi"]]]))))))

(t/deftest add-and-find-by-havainnointikaynti-test
  (let [whoami (add-laatija!)
        ^LocalDate date (LocalDate/now)
        energiatodistus (-> (energiatodistus-test/generate-energiatodistus-2018)
                            (assoc-in [:perustiedot :havainnointikaynti] date))
        id (energiatodistus-test/add-energiatodistus! energiatodistus
                                                      (:id whoami)
                                                      2018)]
    (t/is (= (energiatodistus-test/energiatodistus-with-db-fields
              energiatodistus
              id
              (:id whoami)
              2018)
             (first (search whoami [[["="
                                      "perustiedot.havainnointikaynti"
                                      (.toString date)]]]))))))

(t/deftest add-and-find-by-nimi-and-id-test
  (let [whoami (add-laatija!)
        energiatodistus (-> (energiatodistus-test/generate-energiatodistus-2018)
                            (assoc-in [:perustiedot :nimi] "test"))
        id (energiatodistus-test/add-energiatodistus! energiatodistus
                                                      (:id whoami)
                                                      2018)]
    (t/is (= (energiatodistus-test/energiatodistus-with-db-fields
              energiatodistus
              id
              (:id whoami)
              2018)
             (first (search whoami [[["=" "perustiedot.nimi" "test"]
                                     ["=" "id" id]]]))))))

(defn catch-ex-data [f]
  (try (f) (catch ExceptionInfo e (ex-data e))))

(t/deftest invalid-search-expression
  (t/is (= (:type (catch-ex-data #(search paakayttaja [[[1]]])))
           :schema.core/error))
  (t/is (= (:type (catch-ex-data #(search paakayttaja [[[]]])))
           :schema.core/error))
  (t/is (= (catch-ex-data #(search paakayttaja [[["="]]]))
           {:type :invalid-arguments
            :predicate "="
            :message "Wrong number of arguments: () for predicate: ="}))
  (t/is (= (catch-ex-data #(search paakayttaja [[["=" "id"]]]))
           {:type :invalid-arguments
            :predicate "="
            :message "Wrong number of arguments: (\"id\") for predicate: ="}))
  (t/is (= (catch-ex-data #(search paakayttaja [[["asdf" "id" 1]]]))
           {:type :unknown-predicate :predicate "asdf"
            :message "Unknown predicate: asdf"}))
  (t/is (= (catch-ex-data #(search paakayttaja [[["=" "asdf" "test"]]]))
           {:type :unknown-field
            :field "asdf"
            :message "Unknown field: asdf"})))
