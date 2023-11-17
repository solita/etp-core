(ns solita.etp.energiatodistus-generator
  (:require [clojure.java.jdbc :as jdbc]
            [solita.etp.db :as db]
            [solita.etp.service.energiatodistus :as energiatodistus-service]
            [solita.etp.test-data.energiatodistus :as energiatodistus-test-data]
            [solita.etp.test-data.laatija :as laatija-test-data]
            [solita.etp.test-system :as ts])
  (:import (java.time Instant)))

(defn- energiatodistukset->signed-energiatodistukset-db-rows
  "Transform generated energiatodistukset into db-rows that are signed."
  [laatija-id versio energiatodistukset]
  (map #(-> % (assoc :laatija-id laatija-id
                     :versio versio
                     ;; The members above are items simply not set by the direct
                     ;; generator function, so they are inserted here. The items
                     ;; below mock the signing.
                     ;;
                     ;; This is perhaps not 100% accurate representation on how a signed ET
                     ;; in production data looks, but it should be close enough for
                     ;; performance testing purposes.
                     :tila-id 2
                     :allekirjoitusaika (Instant/now)
                     ;; Set voimassaolo-paattymisaika 10 years into the future.
                     :voimassaolo-paattymisaika (-> (Instant/now)
                                                    (.plusSeconds 315360000)))
            energiatodistus-service/energiatodistus->db-row)
       energiatodistukset)
  )

(defn generate-and-insert-energiatodistukset-for-performance-testing!
  "Adds count `n` signed energiatodistus into the given database `db`.
  The signing is shortcut by just inserting energiatodistus rows, that are signed, into the
  database. Adds the energiatodistukset in batches. Prints the count of energiatodistukset
  inserted and elapsed time after the insertion of every batch. The easiest way to provide
  the database `db` is probably by calling `user/db`. Will also add a new laatija that is then
  put as the laatija of every energiatodistus created by this function. The laatija is generated
  by `solita.etp.test-data.laatija/generate-and-insert!`. The energiatodistukset are generated by
  `solita.etp.test-data.energiatodistus/generate-adds`, but modified to signed before inserting."
  [db n]
  (let [batch-size 100]
    (with-bindings {#'ts/*db* db}
      (let [versio 2018
            [laatija-id _] (-> (laatija-test-data/generate-and-insert! 1) first)
            energiatodistukset (energiatodistus-test-data/generate-adds n versio true)
            energiatodistukset-db-rows (energiatodistukset->signed-energiatodistukset-db-rows laatija-id versio energiatodistukset)]
        (doseq [batch (partition-all batch-size energiatodistukset-db-rows)]
          (time (jdbc/insert-multi!
                  db :energiatodistus
                  (keys (first batch))
                  (map vals batch)
                  db/default-opts))
          (println (str "Added " (count batch) " energiatodistus")))))))