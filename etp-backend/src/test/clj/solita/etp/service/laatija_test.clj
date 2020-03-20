(ns solita.etp.service.laatija-test
  (:require [clojure.test :as t]
            [schema.core :as schema]
            [schema-generators.generators :as g]
            [schema-generators.complete :as c]
            [solita.common.map :as solita-map]
            [solita.etp.test-system :as ts]
            [solita.etp.service.laatija :as service]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.kayttaja :as kayttaja-schema]
            [solita.etp.schema.laatija :as laatija-schema]))

(t/use-fixtures :each ts/fixture)

(defn unique-henkilotunnus-range [to]
  (->> (range 0 to)
       (map (partial format "%09d"))
       (map #(str % (common-schema/henkilotunnus-checksum %)))
       (map #(str (subs % 0 6) "-" (subs % 6 10)))))

(def working-henkilotunnus "130200A892S")

(def laatija-generators
  {common-schema/Henkilotunnus       (g/always working-henkilotunnus)
   laatija-schema/MuutToimintaalueet (g/always [0, 1, 2, 3, 17])
   common-schema/Date                (g/always (java.time.LocalDate/now))})

(defn generate-KayttajaLaatijaAdds [n]
  (map (fn [kayttaja-laatija henkilotunnus]
         (-> kayttaja-laatija
             (assoc-in [:laatija :henkilotunnus] henkilotunnus)
             (assoc-in [:laatija :maa] "FI")))
       (repeatedly n #(g/generate laatija-schema/KayttajaLaatijaAdd
                                  laatija-generators))
       (unique-henkilotunnus-range n)))

(t/deftest add-and-find-test
  (doseq [kayttaja-laatija (generate-KayttajaLaatijaAdds 100)
          :let [_ (service/upsert-kayttaja-laatijat! ts/*db* [kayttaja-laatija])
                found (service/find-kayttaja-laatija-with-henkilotunnus
                       ts/*db* (-> kayttaja-laatija
                                   :laatija
                                   :henkilotunnus))]]
    (schema/validate laatija-schema/KayttajaLaatija found)
    (t/is (solita-map/submap? (:kayttaja kayttaja-laatija) (:kayttaja found)))
    (t/is (solita-map/submap? (:laatija kayttaja-laatija) (:laatija found)))))

(t/deftest add-and-update-existing-test
  (let [kayttaja-laatijat (generate-KayttajaLaatijaAdds 2)
        [original-1 original-2] kayttaja-laatijat
        _ (service/upsert-kayttaja-laatijat! ts/*db* kayttaja-laatijat)
        found-original-1 (service/find-kayttaja-laatija-with-henkilotunnus
                          ts/*db*
                          (-> original-1
                              :laatija
                              :henkilotunnus))
        found-original-2 (service/find-kayttaja-laatija-with-henkilotunnus ts/*db* (-> original-2
                                                                             :laatija
                                                                             :henkilotunnus))
        updated-1 (-> original-1
                      (assoc-in [:kayttaja :etunimi] "This should not update")
                      (assoc-in [:laatija :laatimiskielto] true)
                      (assoc-in [:laatija :julkinenemail] true))
        _ (service/upsert-kayttaja-laatijat! ts/*db* [original-2 updated-1])
        found-updated-1 (service/find-kayttaja-laatija-with-henkilotunnus
                         ts/*db*
                         (-> original-1
                             :laatija
                             :henkilotunnus))
        found-updated-2 (service/find-kayttaja-laatija-with-henkilotunnus
                         ts/*db*
                         (-> original-2
                             :laatija
                             :henkilotunnus))]
    (schema/validate laatija-schema/KayttajaLaatija found-original-1)
    (schema/validate laatija-schema/KayttajaLaatija found-original-2)
    (schema/validate laatija-schema/KayttajaLaatija found-updated-1)
    (schema/validate laatija-schema/KayttajaLaatija found-updated-2)

    ;; Name (or the rest of the käyttäjä) is not be updated!
    (t/is (= (:kayttaja found-original-1) (:kayttaja found-updated-1)))

    ;; Laatija has been updated
    (t/is (not= (:laatija found-original-1) (:kayttaja found-updated-1)))
    (t/is (false? (-> found-original-1 :laatija :laatimiskielto)))
    (t/is (true? (-> found-updated-1 :laatija :laatimiskielto)))
    (t/is (false? (-> found-original-1 :laatija :julkinenemail)))
    (t/is (true? (-> found-updated-1 :laatija :julkinenemail)))

    ;; The second has not changed at all
    (t/is (= found-original-2 found-updated-2))))

(t/deftest find-patevyydet-test
  (let [patevyydet (service/find-patevyystasot)
        fi-labels  (map :label-fi patevyydet)
        se-labels  (map :label-sv patevyydet)]
    (t/is (= ["Perustaso" "Ylempi taso"] fi-labels))
    (t/is (= ["Basnivå" "Högre nivå"] se-labels))))
