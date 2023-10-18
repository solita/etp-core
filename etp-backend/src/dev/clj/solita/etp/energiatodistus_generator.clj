(ns solita.etp.energiatodistus-generator
  (:require [clojure.java.jdbc :as jdbc]
            [solita.etp.db :as db]
            [solita.etp.service.energiatodistus :as energiatodistus-service]
            [solita.etp.test-data.energiatodistus :as energiatodistus-test-data]
            [solita.etp.test-data.laatija :as laatija-test-data]
            [solita.etp.test-system :as ts])
  (:import (java.time Instant)))

;; This is perhaps not 100% accurate representation on how production data
;; looks, but it should be close enough for performance testing purposes.
;; The added energiatodistukset are just set to signed directly.
(defn add-energiatodistukset! [db laatija-id versio energiatodistukset]
  (let [db-rows (map #(-> % (assoc :versio versio
                                   :laatija-id laatija-id
                                   :tila-id 2
                                   :allekirjoitusaika (Instant/now)
                                   :voimassaolo-paattymisaika (-> (Instant/now)
                                                                  (.plusSeconds 315360000)))
                          energiatodistus-service/energiatodistus->db-row)
                     energiatodistukset)]
    (jdbc/insert-multi!
      db :energiatodistus
      (keys (first db-rows))
      (map vals db-rows)
      db/default-opts)))

;; Will also add a laatija for the added energiatodistukset.
(defn add-energiatodistukset-for-performance-testing [db n]
  (let [batch-size 100]
    (when (< n batch-size)
      (binding [*out* *err*]
        (println (str "ERROR: We are generating ETs with batch size of " batch-size ", so the number of ETs must be at least " batch-size "."))))
    (when (not= (rem n batch-size) 0)
      (binding [*out* *err*]
        (println (str "WARNING: The amount to generate (" n ") is not divisible by the batch size (" batch-size "). Only full partitions will be used."))))
    (with-bindings {#'ts/*db* db}
      (let [[laatija-id _] (-> (laatija-test-data/generate-and-insert! 1) first)
            energiatodistukset (energiatodistus-test-data/generate-adds n 2018 true)]
        (doseq [batch (partition batch-size energiatodistukset)]
          (time (add-energiatodistukset! db laatija-id 2018 batch))
          (println (str "Added " batch-size " energiatodistus")))))))
