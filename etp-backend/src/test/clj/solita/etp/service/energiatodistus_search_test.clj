(ns solita.etp.service.energiatodistus-search-test
  (:require [clojure.test :as t]
            [schema-tools.core :as st]
            [solita.etp.test-system :as ts]
            [solita.etp.service.energiatodistus-test :as energiatodistus-test]
            [solita.etp.service.energiatodistus-search :as energiatodistus-search-service]
            [solita.etp.service.energiatodistus :as energiatodistus-service]
            [solita.etp.schema.public-energiatodistus :as public-energiatodistus-schema])
  (:import (clojure.lang ExceptionInfo)
           (java.time LocalDate Instant ZoneId)))

(t/use-fixtures :each ts/fixture)

(defn add-laatija! []
  {:id (energiatodistus-test/add-laatija!)
   :rooli 0})

(def paakayttaja {:rooli 2})

(defn search [whoami where keyword]
  (map #(dissoc % :laatija-fullname)
       (energiatodistus-search-service/search
        ts/*db*
        whoami
        (cond-> {}
          where (assoc :where where)
          keyword (assoc :keyword keyword)))))

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
    (t/is (empty? (search whoami [[["=" "energiatodistus.id" -1]]] nil)))))

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
             (first (search whoami [[["=" "energiatodistus.id" id]]] nil))))))

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
             (first (search whoami
                            [[["=" "energiatodistus.perustiedot.nimi" "test"]]]
                            nil))))))

(t/deftest add-and-find-by-toimintaalue-test
  (let [whoami (add-laatija!)
        energiatodistus (-> (energiatodistus-test/generate-energiatodistus-2018)
                            (assoc-in [:perustiedot :postinumero] "33100"))
        id (energiatodistus-test/add-energiatodistus! energiatodistus
                                                      (:id whoami)
                                                      2018)
        energiatodistus-with-db-fields (public-energiatodistus-with-db-fields
                                        energiatodistus
                                        id
                                        (:id whoami)
                                        2018)]
    (t/is (empty? (search whoami
                          [[["like" "toimintaalue.label-fi" "Kain"]]]
                          nil)))
    (t/is (empty? (search whoami nil "Kain")))
    (t/is (= energiatodistus-with-db-fields
             (first (search whoami
                            [[["like" "toimintaalue.label-fi" "Pirkanma%"]]]
                            nil))))
    (t/is (= energiatodistus-with-db-fields
             (first (search whoami
                            nil
                            "Pirkanm"))))
    (t/is (= energiatodistus-with-db-fields
             (first (search whoami
                            [[["=" "energiatodistus.id" id]]]
                            "Pirkanm"))))))

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
             (first (search whoami
                            [[["nil?" "energiatodistus.perustiedot.nimi"]]]
                            nil))))))

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
             (first (search whoami
                            [[["="
                               "energiatodistus.perustiedot.havainnointikaynti"
                               (.toString date)]]]
                            nil))))))

(defn voimassa-paattymisaika [allekirjoitus-date]
  (-> allekirjoitus-date
      (.plusYears 10)
      (.plusDays 1)
      (.atStartOfDay (ZoneId/of "Europe/Helsinki"))
      (.toInstant)))

(t/deftest add-and-find-by-allekirjoisaika-test
  (let [whoami (add-laatija!)
        energiatodistus (energiatodistus-test/generate-energiatodistus-2018-complete)
        id (energiatodistus-test/add-energiatodistus-and-sign!
             energiatodistus (:id whoami))]

    (t/is (= (assoc (public-energiatodistus-with-db-fields energiatodistus
                                                    id
                                                    (:id whoami)
                                                    2018)
               :tila-id 2,
               :voimassaolo-paattymisaika
               (voimassa-paattymisaika (LocalDate/now)))
             (first (search whoami
                            [[["<"
                               "energiatodistus.allekirjoitusaika"
                               (str (Instant/now))]]]
                            nil))))))

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
             (first (search whoami
                            [[["=" "energiatodistus.perustiedot.nimi" "test"]
                              ["=" "energiatodistus.id" id]]]
                            nil))))))

(t/deftest laatija-cant-find-other-laatijas-energiatodistukset-test
  (let [adder (add-laatija!)
        searcher (update adder :id inc)
        energiatodistus (energiatodistus-test/generate-energiatodistus-2018)
        id (energiatodistus-test/add-energiatodistus! energiatodistus
                                                      (:id adder)
                                                      2018)]
    (t/is (empty? (search searcher [[["=" "energiatodistus.id" id]]] nil)))))

(t/deftest paakayttaja-cant-find-luonnokset-test
  (let [laatija (add-laatija!)
        energiatodistus (energiatodistus-test/generate-energiatodistus-2018)
        id (energiatodistus-test/add-energiatodistus! energiatodistus
                                                      (:id laatija)
                                                      2018)]
    (t/is (empty? (search paakayttaja [[["=" "energiatodistus.id" id]]] nil)))))

(t/deftest deleted-are-not-found-test
  (let [laatija (add-laatija!)
        energiatodistus (energiatodistus-test/generate-energiatodistus-2018)
        id (energiatodistus-test/add-energiatodistus! energiatodistus
                                                      (:id laatija)
                                                      2018)]
    (energiatodistus-service/delete-energiatodistus-luonnos! ts/*db* laatija id)
    (t/is (empty? (search laatija [[["=" "energiatodistus.id" id]]] nil)))
    (t/is (empty? (search paakayttaja [[["=" "energiatodistus.id" id]]] nil)))))

(defn catch-ex-data [f]
  (try (f) (catch ExceptionInfo e (ex-data e))))

(t/deftest invalid-search-expression
  (t/is (= (:type (catch-ex-data #(search paakayttaja [[[1]]] nil)))
           :schema.core/error))
  (t/is (= (:type (catch-ex-data #(search paakayttaja [[[]]] nil)))
           :schema.core/error))
  (t/is (= (catch-ex-data #(search paakayttaja [[["="]]] nil))
           {:type :invalid-arguments
            :predicate "="
            :message "Wrong number of arguments: () for predicate: ="}))
  (t/is (= (catch-ex-data #(search paakayttaja [[["=" "id"]]] nil))
           {:type :invalid-arguments
            :predicate "="
            :message "Wrong number of arguments: (\"id\") for predicate: ="}))
  (t/is (= (catch-ex-data #(search paakayttaja [[["asdf" "id" 1]]] nil))
           {:type :unknown-predicate :predicate "asdf"
            :message "Unknown predicate: asdf"}))
  (t/is (= (catch-ex-data #(search paakayttaja [[["=" "asdf" "test"]]] nil))
           {:type :unknown-field
            :field "asdf"
            :message "Unknown field: asdf"})))
