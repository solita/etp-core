(ns solita.etp.service.laskutus-test
  (:require [clojure.string :as str]
            [clojure.test :as t]
            [clojure.java.jdbc :as jdbc]
            [solita.common.map :as xmap]
            [solita.etp.test-system :as ts]
            [solita.etp.test-data.yritys :as yritys-test-data]
            [solita.etp.test-data.laatija :as laatija-test-data]
            [solita.etp.test-data.energiatodistus :as energiatodistus-test-data]
            [solita.etp.service.energiatodistus :as energiatodistus-service]
            [solita.etp.service.laskutus :as laskutus-service]))

(t/use-fixtures :each ts/fixture)

;; Yritys 1 has laatija 1.
;; Yritys 2 has laatija 2.
;; Laatija 3 and 4 have no yritys.
;; Laatija 1 has energiatodistukset 1 and 5, laatija 2 has 2 and 6 etc.
;; Laskut from energiatodistukset 1 and 5 should go to yritys 1.
;; Laskut from energiatodistukset 2 and 6 should go to yritys 2.
;; Laskut energiatodistukset 3, 4, 7 and 8 should go their laatijat.
;; Energiatodistukset 1-7 are signed.
;; Energiatodistukset 1-6 are signed during last month.

(defn test-data-set []
  (let [laatijat (laatija-test-data/generate 4)
        laatija-ids (laatija-test-data/insert! laatijat)
        yritykset (yritys-test-data/generate 2)
        yritys-ids (->> (interleave laatija-ids yritykset)
                        (partition 2)
                        (mapcat #(yritys-test-data/insert!
                                  {:id (first %)}
                                  [(second %)])))
        energiatodistukset (->> (interleave
                                 (energiatodistus-test-data/generate 4 2013 true)
                                 (energiatodistus-test-data/generate 4 2018 true))
                                (interleave (cycle (concat yritys-ids [nil nil])))
                                (partition 2)
                                (map #(assoc (second %)
                                             :laskutettava-yritys-id
                                             (first %))))
        energiatodistus-ids (->> (interleave (cycle laatija-ids)
                                             energiatodistukset)
                                 (partition 2)
                                 (mapcat #(energiatodistus-test-data/insert!
                                           {:id (first %)}
                                           [(second %)]))
                                 (map :id)
                                 doall)]
    (doseq [[laatija-id energiatodistus-id] (->> (interleave (cycle laatija-ids)
                                                             energiatodistus-ids)
                                                 (partition 2)
                                                 (take 7))]
      (energiatodistus-service/start-energiatodistus-signing! ts/*db*
                                                              {:id laatija-id}
                                                              energiatodistus-id)
      (energiatodistus-service/end-energiatodistus-signing! ts/*db*
                                                            {:id laatija-id}
                                                            energiatodistus-id))
    (jdbc/execute! ts/*db* ["UPDATE energiatodistus SET allekirjoitusaika = allekirjoitusaika - interval '1 month' WHERE id <= 6"])
    {:laatijat (apply assoc {} (interleave laatija-ids laatijat))
     :yritykset (apply assoc {} (interleave yritys-ids yritykset))
     :energiatodistukset (apply assoc {} (interleave energiatodistus-ids
                                                     energiatodistukset))}))

(t/deftest find-kuukauden-laskutus-test
  (let [_ (test-data-set)
        laskutus (laskutus-service/find-kuukauden-laskutus ts/*db*)]
    (t/is (= 6 (count laskutus)))))

(t/deftest asiakastiedot-test
  (let [{:keys [yritykset laatijat]} (test-data-set)
        laskutus (laskutus-service/find-kuukauden-laskutus ts/*db*)
        asiakastiedot (laskutus-service/asiakastiedot laskutus)]
    (t/is (= 4 (count asiakastiedot)))
    (t/is (= (merge (->> laatijat
                         keys
                         sort
                         (take 2)
                         (apply dissoc laatijat)
                         (reduce-kv (fn [acc id {:keys [etunimi sukunimi]}]
                                      (assoc acc
                                             (format "L0%08d" id)
                                             (str etunimi " " sukunimi)))
                                    {}))
                    (reduce-kv (fn [acc id {:keys [nimi]}]
                                 (assoc acc
                                        (format "L1%08d" id)
                                        nimi))
                               {}
                               yritykset))
             (xmap/map-values :nimi asiakastiedot)))))

(t/deftest asiakastiedot-xml-test
  (let [{:keys [yritykset]} (test-data-set)
        laskutus (laskutus-service/find-kuukauden-laskutus ts/*db*)
        asiakastiedot (laskutus-service/asiakastiedot laskutus)
        yritys-id (-> yritykset keys sort first)
        yritys (get yritykset yritys-id)
        asiakastunnus (format "L1%08d" yritys-id)
        asiakastieto (get asiakastiedot (format "L1%08d" yritys-id))
        xml-str (laskutus-service/asiakastieto-xml asiakastieto)]
    (t/is (str/includes? xml-str (str "<AsiakasTunnus>"
                                      asiakastunnus
                                      "</AsiakasTunnus")))
    (t/is (str/includes? xml-str (str "<LahiOsoite>"
                                      (:jakeluosoite yritys)
                                      "</LahiOsoite")))
    (t/is (str/includes? xml-str "<KumppaniNro>ETP</KumppaniNro>"))))
