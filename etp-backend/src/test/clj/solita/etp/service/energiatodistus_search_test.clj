(ns solita.etp.service.energiatodistus-search-test
  (:require [clojure.test :as t]
            [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [schema-generators.generators :as g]
            [solita.etp.test-system :as ts]
            [solita.etp.schema.energiatodistus :as schema]
            [solita.etp.service.json :as json]
            [solita.etp.service.energiatodistus-search :as service]
            [solita.etp.service.energiatodistus-test :as energiatodistus-test]))

(t/use-fixtures :each ts/fixture)

(defn generate-energiatodistukset [n]
  (repeatedly n #(g/generate schema/EnergiatodistusSave2018
                             energiatodistus-test/energiatodistus-generators)))

(defn add-energiatodistukset! [energiatodistukset laatija-id]
  (doseq [batch (->> energiatodistukset
                     (map #(vector 2018 laatija-id (json/write-value-as-string %)))
                     (partition-all 1000))]
    (jdbc/execute!
     (ts/db-user laatija-id)
     (concat
      ["INSERT INTO energiatodistus (versio, laatija_id, data) VALUES (?, ?, ? :: JSONB) returning id"]
      batch)
     {:multi? true})))

(defn update-energiatodistus [energiatodistus katuosoite-fi pohjoinen-ikkuna-ala
                              lammitys-eluvun-muutos]
  (-> energiatodistus
      (assoc-in [:perustiedot :katuosoite-fi] katuosoite-fi)
      (assoc-in [:lahtotiedot :ikkunat :pohjoinen :ala] pohjoinen-ikkuna-ala)
      (assoc-in [:huomiot :lammitys :toimenpide 0 :eluvun-muutos] lammitys-eluvun-muutos)))

(defn get-table-size [table-name]
  (-> ts/*db*
      (jdbc/query ["SELECT pg_size_pretty(pg_total_relation_size(?))"
                   table-name])
      first
      :pg_size_pretty))

(def not-to-be-found-energiatodistukset
  (->> 10
       generate-energiatodistukset
       (map #(update-energiatodistus % "Itsenäisyydenkatu 1 A 2" 200 200))
       cycle))

(def to-be-found-energiatodistukset
  (->> 100
       generate-energiatodistukset
       (map #(update-energiatodistus % "Hämeenkatu 1 A 2" 100 100))
       cycle))

(def katuosoite-fi-query ["ilike"
                          [:perustiedot :katuosoite-fi]
                          "%Hämeenkatu%"])
(def katuosoite-fi-sql "data->'perustiedot'->>'katuosoite-fi' ilike ?")
(def ikkuna-ala-query ["<" [:lahtotiedot :ikkunat :pohjoinen :ala] 150])
(def ikkuna-ala-sql "(data->'lahtotiedot'->'ikkunat'->'pohjoinen'->>'ala')::numeric < ?")
(def eluvun-muutos-query ["="
                          [:huomiot :lammitys :toimenpide 0 :eluvun-muutos]
                          100])
(def eluvun-muutos-sql "(data->'huomiot'->'lammitys'->'toimenpide'->0->>'eluvun-muutos')::numeric = ?")

(t/deftest k->sql-test
  (t/is (= (service/k->sql :perustiedot) "'perustiedot'"))
  (t/is (= (service/k->sql 0) "0")))

(t/deftest path->sql-test
  (t/is (= (service/path->sql [:huomiot :lammitys :toimenpide 0 :eluvun-muutos])
           "data->'huomiot'->'lammitys'->'toimenpide'->0->>'eluvun-muutos'")))

(t/deftest query-part->sql-test
  (t/is (= (service/query-part->sql katuosoite-fi-query) katuosoite-fi-sql))
  (t/is (= (service/query-part->sql ikkuna-ala-query) ikkuna-ala-sql))
  (t/is (= (service/query-part->sql eluvun-muutos-query) eluvun-muutos-sql)))

(t/deftest or-query->sql-and-params-test
  (t/is (= (service/or-query->sql-and-params [katuosoite-fi-query])
           {:sql katuosoite-fi-sql
            :params ["%Hämeenkatu%"]}))
  (t/is (= (service/or-query->sql-and-params [ikkuna-ala-query
                                              eluvun-muutos-query])
           {:sql (str ikkuna-ala-sql " OR " eluvun-muutos-sql)
            :params [150 100]})))

(t/deftest and-query->sql-and-params-test
  (t/is (= (service/and-query->sql-and-params [[katuosoite-fi-query]])
           {:sql (format "(%s)" katuosoite-fi-sql)
            :params ["%Hämeenkatu%"]}))
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
            "%Hämeenkatu%" 150 100])))

(t/deftest search-test
  (let [laatija-id (energiatodistus-test/add-laatija!)
        energiatodistukset (concat (take 10000 not-to-be-found-energiatodistukset)
                                   (take 100 to-be-found-energiatodistukset))
        _ (add-energiatodistukset! energiatodistukset laatija-id)
        results (service/search ts/*db* [[katuosoite-fi-query]
                                         [ikkuna-ala-query]
                                         [eluvun-muutos-query]])]
    (t/is (= (count results) 100))))

;; Commented because this is a slow test
#_(t/deftest performance-test
  (let [laatija-id (energiatodistus-test/add-laatija!)
        energiatodistukset (concat (take 1000000 not-to-be-found-energiatodistukset)
                                   (take 100 to-be-found-energiatodistukset))
        _ (log/info "Size of table in the beginning" (get-table-size "energiatodistus"))
        _ (log/info "Energiatodistukset has been generated")
        _ (add-energiatodistukset! energiatodistukset laatija-id)
        _ (log/info "Energiatodistukset has been inserted to db")
        _ (log/info "Size of table after inserting: " (get-table-size "energiatodistus"))
        before-search-1 (System/currentTimeMillis)
        results-1 (service/search ts/*db* [[katuosoite-fi-query]
                                           [ikkuna-ala-query]
                                           [eluvun-muutos-query]])
        after-search-1 (System/currentTimeMillis)
        result-count-1 (count results-1)
        _ (log/info "1. search completed. The count of results was " result-count-1)
        _ (log/info "1. search took " (- after-search-1 before-search-1) " ms")

        before-search-2 (System/currentTimeMillis)
        results-2 (service/search ts/*db* [[katuosoite-fi-query]
                                           [ikkuna-ala-query]
                                           [eluvun-muutos-query]])
        after-search-2 (System/currentTimeMillis)
        result-count-2 (count results-2)
        _ (log/info "2. search completed. The count of results was " result-count-2)
        _ (log/info "2. search took " (- after-search-2 before-search-2) "ms")]
    (t/is (= (count results-1) 100))
    (t/is (= (count results-2) 100))))

;;
;; Concepts for how fields could be renamed, deleted or added
;;

(defn migration-path [path]
  (format "{%s}" (->> path
                      (map #(if (keyword? %) (name %) %))
                      (str/join ", "))))

(def add-field-sql "UPDATE energiatodistus SET data =
                    jsonb_set(data, '{perustiedot, added}', 100::text::jsonb)")
(def rename-field-sql "UPDATE energiatodistus SET data =
                       jsonb_set(data #- '{perustiedot, added}',
                                 '{perustiedot, renamed}',
                                 data->'perustiedot'->'added')")


(def added-query [[["=" [:perustiedot :added] 100]]])
(def renamed-query [[["=" [:perustiedot :renamed] 100]]])

(t/deftest json-migration-test
  (let [laatija-id (energiatodistus-test/add-laatija!)
        energiatodistukset (take 100 not-to-be-found-energiatodistukset)
        _ (add-energiatodistukset! energiatodistukset laatija-id)
        _ (jdbc/execute! ts/*db* [add-field-sql])
        added-results-1 (service/search ts/*db* added-query)
        _ (jdbc/execute! ts/*db* [rename-field-sql])
        added-results-2 (service/search ts/*db* added-query)
        renamed-results (service/search ts/*db* renamed-query)]
    (t/is (= (count added-results-1) 100))
    (t/is (= (count added-results-2) 0))
    (t/is (= (count renamed-results) 100))))
