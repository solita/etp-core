(ns solita.etp.service.energiatodistus-search-test
  (:require [clojure.test :as t]
            [schema-tools.core :as st]
            [solita.etp.test-system :as ts]
            [solita.etp.service.energiatodistus-test :as energiatodistus-test]
            [solita.etp.service.energiatodistus-search :as energiatodistus-search-service]
            [solita.etp.service.energiatodistus :as energiatodistus-service]
            [solita.etp.schema.public-energiatodistus :as public-energiatodistus-schema])
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

(defn public-energiatodistus-with-db-fields
  [energiatodistus id laatija-id versio]
  (-> energiatodistus
      (energiatodistus-test/energiatodistus-with-db-fields id
                                                           laatija-id
                                                           versio)
      (assoc :laatija-fullname "")
      (st/select-schema public-energiatodistus-schema/Energiatodistus)
      (dissoc :laatija-fullname)))

(t/deftest not-found-test
  (let [whoami (add-laatija!)]
    (-> (energiatodistus-test/generate-energiatodistus-2018)
        (energiatodistus-test/add-energiatodistus! (:id whoami) 2018))
    (t/is (empty? (search whoami [[["=" "energiatodistus.id" -1]]])))))

(t/deftest add-and-find-by-id-test
  (let [whoami (add-laatija!)
        energiatodistus (energiatodistus-test/generate-energiatodistus-2018)
        id (energiatodistus-test/add-energiatodistus! energiatodistus
                                                      (:id whoami)
                                                      2018)]
    (t/is (= (public-energiatodistus-with-db-fields energiatodistus
                                                    id
                                                    (:id whoami)
                                                    2018)
             (->> [[["=" "energiatodistus.id" id]]]
                  (search whoami)
                  first)))))

(t/deftest add-and-find-by-nimi-test
  (let [whoami (add-laatija!)
        energiatodistus (-> (energiatodistus-test/generate-energiatodistus-2018)
                            (assoc-in [:perustiedot :nimi] "test"))
        id (energiatodistus-test/add-energiatodistus! energiatodistus
                                                      (:id whoami)
                                                      2018)]
    (t/is (= (public-energiatodistus-with-db-fields energiatodistus
                                                    id
                                                    (:id whoami)
                                                    2018)
             (->> [[["=" "energiatodistus.perustiedot.nimi" "test"]]]
                  (search whoami)
                  first)))))

(t/deftest add-and-find-by-nimi-nil-test
  (let [whoami (add-laatija!)
        energiatodistus (-> (energiatodistus-test/generate-energiatodistus-2018)
                            (assoc-in [:perustiedot :nimi] nil))
        id (energiatodistus-test/add-energiatodistus! energiatodistus
                                                      (:id whoami)
                                                      2018)]
    (t/is (= (public-energiatodistus-with-db-fields energiatodistus
                                                    id
                                                    (:id whoami)
                                                    2018)
             (->> [[["nil?" "energiatodistus.perustiedot.nimi"]]]
                  (search whoami)
                  first)))))

(t/deftest add-and-find-by-havainnointikaynti-test
  (let [whoami (add-laatija!)
        ^LocalDate date (LocalDate/now)
        energiatodistus (-> (energiatodistus-test/generate-energiatodistus-2018)
                            (assoc-in [:perustiedot :havainnointikaynti] date))
        id (energiatodistus-test/add-energiatodistus! energiatodistus
                                                      (:id whoami)
                                                      2018)]
    (t/is (= (public-energiatodistus-with-db-fields energiatodistus
                                                    id
                                                    (:id whoami)
                                                    2018)
             (->> [[["="
                     "energiatodistus.perustiedot.havainnointikaynti"
                     (.toString date)]]]
                  (search whoami)
                  (first ))))))

(t/deftest add-and-find-by-nimi-and-id-test
  (let [whoami (add-laatija!)
        energiatodistus (-> (energiatodistus-test/generate-energiatodistus-2018)
                            (assoc-in [:perustiedot :nimi] "test"))
        id (energiatodistus-test/add-energiatodistus! energiatodistus
                                                      (:id whoami)
                                                      2018)]
    (t/is (= (public-energiatodistus-with-db-fields energiatodistus
                                                    id
                                                    (:id whoami)
                                                    2018)
             (->> [[["=" "energiatodistus.perustiedot.nimi" "test"]
                    ["=" "energiatodistus.id" id]]]
                  (search whoami)
                  first)))))

(t/deftest laatija-cant-find-other-laatijas-energiatodistukset-test
  (let [adder (add-laatija!)
        searcher (update adder :id inc)
        energiatodistus (energiatodistus-test/generate-energiatodistus-2018)
        id (energiatodistus-test/add-energiatodistus! energiatodistus
                                                      (:id adder)
                                                      2018)]
    (t/is (empty? (search searcher [[["=" "energiatodistus.id" id]]])))))

(t/deftest paakayttaja-cant-find-luonnokset-test
  (let [laatija (add-laatija!)
        energiatodistus (energiatodistus-test/generate-energiatodistus-2018)
        id (energiatodistus-test/add-energiatodistus! energiatodistus
                                                      (:id laatija)
                                                      2018)]
    (t/is (empty? (search paakayttaja [[["=" "energiatodistus.id" id]]])))))

(t/deftest deleted-are-not-found-test
  (let [laatija (add-laatija!)
        energiatodistus (energiatodistus-test/generate-energiatodistus-2018)
        id (energiatodistus-test/add-energiatodistus! energiatodistus
                                                      (:id laatija)
                                                      2018)]
    (energiatodistus-service/delete-energiatodistus-luonnos! ts/*db* laatija id)
    (t/is (empty? (search laatija [[["=" "energiatodistus.id" id]]])))
    (t/is (empty? (search paakayttaja [[["=" "energiatodistus.id" id]]])))))

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
