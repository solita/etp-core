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

(defn add-energiatodistukset-for-performance-testing [db n]
  (when (< n 100)
    (println "We are generating ETs with batch size of 100, so the number of ETs must be at least 100."))
  (with-bindings {#'ts/*db* db}
    (let [[laatija-id _] (-> (laatija-test-data/generate-and-insert! 1) first)
          energiatodistukset (energiatodistus-test-data/generate-adds n 2018 true)]
      (doseq [batch (partition 100 energiatodistukset)]
        (time (add-energiatodistukset! db laatija-id 2018 batch))))))
