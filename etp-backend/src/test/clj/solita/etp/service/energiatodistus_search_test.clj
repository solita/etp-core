(ns solita.etp.service.energiatodistus-search-test
  (:require [clojure.test :as t]
            [clojure.string :as str]
            [schema-generators.generators :as g]
            [solita.etp.test-system :as ts]
            [solita.etp.schema.energiatodistus :as schema]
            [solita.etp.service.energiatodistus-search :as service]
            [solita.etp.service.energiatodistus-test :as energiatodistus-test]))

(t/use-fixtures :each ts/fixture)

(defn generate-energiatodistukset [n]
  (repeatedly n #(g/generate schema/EnergiatodistusSave2018
                             energiatodistus-test/energiatodistus-generators)))

(defn add-energiatodistukset! [energiatodistukset laatija-id]
  (doall (map #(energiatodistus-test/add-energiatodistus! % laatija-id)
              energiatodistukset)))

(defn update-energiatodistus [energiatodistus katuosoite-fi pohjoinen-ikkuna-ala
                              lammitys-eluvun-muutos]
  (-> energiatodistus
      (assoc-in [:perustiedot :katuosoite-fi] katuosoite-fi)
      (assoc-in [:lahtotiedot :ikkunat :pohjoinen :ala] pohjoinen-ikkuna-ala)
      (assoc-in [:huomiot :lammitys :toimenpide 0 :eluvun-muutos] lammitys-eluvun-muutos)))

(def not-to-be-found-energiatodistukset
  (->> 1
       generate-energiatodistukset
       (map #(update-energiatodistus % "Itsenäisyydenkatu 1 A 2" 200 200))))

(def to-be-found-energiatodistukset
  (->> 1
       generate-energiatodistukset
       (map #(update-energiatodistus % "Hämeenkatu 1 A 2" 100 100))))

(def energiatodistukset (concat not-to-be-found-energiatodistukset
                                to-be-found-energiatodistukset))

(def katuosoite-fi-query ["ilike"
                          [:perustiedot :katuosoite-fi]
                          "%Hämeenkatu"])
(def katuosoite-fi-sql "data->'perustiedot'->'katuosoite-fi' ilike ?")
(def ikkuna-ala-query ["<" [:lahtotiedot :ikkunat :pohjoinen :ala] 150])
(def ikkuna-ala-sql "data->'lahtotiedot'->'ikkunat'->'pohjoinen'->'ala' < ?")
(def eluvun-muutos-query ["="
                          [:huomiot :lammitys :toimenpide 0 :eluvun-muutos]
                          100])
(def eluvun-muutos-sql "data->'huomiot'->'lammitys'->'toimenpide'->0->'eluvun-muutos' = ?")

(t/deftest k->sql-test
  (t/is (= (service/k->sql :perustiedot) "'perustiedot'"))
  (t/is (= (service/k->sql 0) "0")))

(t/deftest path->sql-test
  (t/is (= (service/path->sql [:huomiot :lammitys :toimenpide 0 :eluvun-muutos])
           (str/replace eluvun-muutos-sql #" = \?" ""))))

(t/deftest query-part->sql-test
  (t/is (= (service/query-part->sql katuosoite-fi-query) katuosoite-fi-sql))
  (t/is (= (service/query-part->sql ikkuna-ala-query) ikkuna-ala-sql))
  (t/is (= (service/query-part->sql eluvun-muutos-query) eluvun-muutos-sql)))

(t/deftest or-query->sql-and-params-test
  (t/is (= (service/or-query->sql-and-params [katuosoite-fi-query])
           {:sql katuosoite-fi-sql
            :params ["%Hämeenkatu"]}))
  (t/is (= (service/or-query->sql-and-params [ikkuna-ala-query
                                              eluvun-muutos-query])
           {:sql (str ikkuna-ala-sql " OR " eluvun-muutos-sql)
            :params [150 100]})))

(t/deftest and-query->sql-and-params-test
  (t/is (= (service/and-query->sql-and-params [[katuosoite-fi-query]])
           {:sql (format "(%s)" katuosoite-fi-sql)
            :params ["%Hämeenkatu"]}))
  (t/is (= (service/and-query->sql-and-params [[ikkuna-ala-query]
                                               [eluvun-muutos-query]])
           {:sql (format "(%s) AND (%s)" ikkuna-ala-sql eluvun-muutos-sql)
            :params [150 100]})))

(t/deftest query->sql-test
  (t/is (nil? (service/query->sql [])))
  (t/is (= (service/query->sql [[katuosoite-fi-query]
                                [ikkuna-ala-query eluvun-muutos-query]])
           [(format "%s(%s) AND (%s OR %s)"
                    service/base-query
                    katuosoite-fi-sql
                    ikkuna-ala-sql
                    eluvun-muutos-sql)
            "%Hämeenkatu" 150 100])))
