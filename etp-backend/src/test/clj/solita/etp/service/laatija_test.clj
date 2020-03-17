(ns solita.etp.service.laatija-test
  (:require [clojure.test :as t]
            [schema-generators.generators :as g]
            [solita.etp.test-system :as ts]
            [solita.etp.service.laatija :as service]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.laatija :as laatija-schema]
            [solita.etp.schema.laatija :as schema]))

(t/use-fixtures :each ts/fixture)

(defn unique-henkilotunnus-range [to]
  (->> (range 0 to)
       (map (partial format "%09d"))
       (map #(str % (common-schema/henkilotunnus-checksum %)))
       (map #(str (subs % 0 6) "-" (subs % 6 10)))))

(def laatija-generators {common-schema/Henkilotunnus      (g/always "130200A892S")
                         laatija-schema/MuutToimintaalueet
                                                          (g/always [0, 1, 2, 3, 17])
                         common-schema/Date               (g/always (java.time.LocalDate/now))})

(t/deftest add-and-find-test
  (let [laatija-count             100
        henkilotunnukset          (unique-henkilotunnus-range laatija-count)
        laatijat                  (repeatedly laatija-count #(g/generate schema/LaatijaSave laatija-generators))
        laatijat-with-unique-hetu (mapv (fn [laatija henkilotunnus]
                                          (assoc laatija :henkilotunnus henkilotunnus)) laatijat henkilotunnukset)
        added-laatijat            (service/add-or-update-existing-laatijat! ts/*db* laatijat-with-unique-hetu)]
    (doseq [[idx laatija-id] (map-indexed vector added-laatijat)]
      (t/is (= (assoc (get laatijat-with-unique-hetu idx) :id laatija-id) (service/find-laatija ts/*db* laatija-id))))))

(t/deftest add-and-update-existing-test
  (let [laatija                     (g/generate schema/LaatijaSave laatija-generators)
        laatija-update              (g/generate schema/LaatijaSave laatija-generators)
        laatija-not-update          (assoc (g/generate schema/LaatijaSave laatija-generators) :henkilotunnus "131052-308T")

        laatija-added               (first (service/add-or-update-existing-laatijat! ts/*db* [laatija]))
        laatija-update-updated      (first (service/add-or-update-existing-laatijat! ts/*db* [laatija-update]))
        laatija-not-update-updated      (first (service/add-or-update-existing-laatijat! ts/*db* [laatija-not-update]))]
    (t/is (= (assoc laatija :id 1) (service/find-laatija ts/*db* laatija-added)))
    (t/is (= (-> (service/find-laatija ts/*db* laatija-added)
                 (merge (select-keys laatija-update [:patevyys :patevyys-voimassaoloaika])))
             (service/find-laatija ts/*db* laatija-update-updated)))
    (t/is (= (assoc laatija-not-update :id 2) (service/find-laatija ts/*db* laatija-not-update-updated)))
    (t/is (= 2 (count (service/find-all-laatijat ts/*db*))))))

(t/deftest find-patevyydet-test
  (let [patevyydet (service/find-patevyystasot)
        fi-labels  (map :label-fi patevyydet)
        se-labels  (map :label-sv patevyydet)]
    (t/is (= ["Perustaso" "Ylempi taso"] fi-labels))
    (t/is (= ["Basnivå" "Högre nivå"] se-labels))))