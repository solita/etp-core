(ns solita.etp.service.energiatodistus-test
  (:require [clojure.test :as t]
            [schema-generators.generators :as g]
            [solita.etp.test-system :as ts]
            [solita.etp.service.energiatodistus :as service]
            [solita.etp.service.kayttaja-laatija :as laatija-service]
            [solita.etp.service.kayttaja-laatija-test :as laatija-service-test]
            [solita.etp.schema.energiatodistus :as schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.geo :as geo-schema]
            [clojure.java.jdbc :as jdbc]
            [solita.etp.db :as db])
  (:import [java.time Instant]))

(t/use-fixtures :each ts/fixture)

(def energiatodistus-generators
  {schema.core/Num          (g/always 1.0M)
   common-schema/Year       (g/always 2021)
   schema/Rakennustunnus    (g/always "1234567890")
   schema/YritysPostinumero (g/always "00100")
   common-schema/Date       (g/always (java.time.LocalDate/now))
   common-schema/Integer100 (g/always 50)
   geo-schema/Postinumero   (g/always "00100")
   common-schema/Instant    (g/always (Instant/now))
   (schema.core/eq 2018)    (g/always 2018)})

(defn add-laatija!
  ([] (add-laatija! ts/*db*))
  ([db]
    (-> (laatija-service/upsert-kayttaja-laatijat!
          db (laatija-service-test/generate-KayttajaLaatijaAdds 1))
        first)))

(defn add-energiatodistus!
  ([energiatodistus laatija-id]
   (add-energiatodistus! (ts/db-user laatija-id) energiatodistus laatija-id))
  ([db energiatodistus laatija-id]
    (service/add-energiatodistus! db {:id laatija-id} 2018 energiatodistus)))

(defn find-energiatodistus [id]
  (let [et (service/find-energiatodistus ts/*db* id)]
    (t/is (not (nil? (:laatija-fullname et))))
    (dissoc et :laatija-fullname)))

(defn complete-energiatodistus [energiatodistus id laatija-id]
  (merge energiatodistus
         {:id id
          :laatija-id laatija-id
          :versio 2018
          :tila-id 0
          :allekirjoitusaika nil}))

(defn generate-energiatodistus []
  (-> (g/generate schema/EnergiatodistusSave2018
                  energiatodistus-generators)
      (assoc :korvattu-energiatodistus-id nil)))

(defn add-energiatodistukset! [db laatija-id versio energiatodistukset]
  (let [db-rows (map #(-> % (assoc :versio versio :laatija-id laatija-id)
                          service/energiatodistus->db-row)
                     energiatodistukset)]
    (jdbc/insert-multi!
      db :energiatodistus
      (keys (first db-rows))
      (map vals db-rows)
      db/default-opts)))

#_(t/deftest add-energiatodistukset-for-performance-testing
  (let [laatija-id (add-laatija!)
        energiatodistukset (vec (apply pcalls (repeat 5000 generate-energiatodistus)))
        db (ts/db-user laatija-id)]
    (time
      (doseq [batch (partition 1000 energiatodistukset)]
        (time (add-energiatodistukset! db laatija-id 2018 batch))))))

(t/deftest add-and-find-energiatodistus-test
  (let [laatija-id (add-laatija!)]
    (doseq [energiatodistus (repeatedly 100 generate-energiatodistus)
            :let [id (add-energiatodistus! energiatodistus laatija-id)]]
      (t/is (= (complete-energiatodistus energiatodistus id laatija-id)
               (find-energiatodistus id))))))

(t/deftest permissions-test
  (let [patevyydentoteaja {:rooli 1}
        paakayttaja {:rooli 2}
        laatija-id (add-laatija!)
        energiatodistus (generate-energiatodistus)
        id (add-energiatodistus! energiatodistus laatija-id)]
    (t/is (= (complete-energiatodistus energiatodistus id laatija-id)
             (-> (service/find-energiatodistus ts/*db* paakayttaja id)
                 (dissoc :laatija-fullname))))
    (t/is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Forbidden"
                            (service/find-energiatodistus ts/*db* patevyydentoteaja id)))))

(t/deftest update-energiatodistus-test
  (let [laatija-id (add-laatija!)
        id (add-energiatodistus! (generate-energiatodistus) laatija-id)
        update-energiatodistus (generate-energiatodistus)]
    (service/update-energiatodistus-luonnos! ts/*db* {:id laatija-id} id update-energiatodistus)
    (t/is (= (complete-energiatodistus update-energiatodistus id laatija-id)
             (find-energiatodistus id)))))

(t/deftest create-energiatodistus-and-delete-test
  (let [laatija-id (add-laatija!)
        id (add-energiatodistus! (generate-energiatodistus) laatija-id)]
    (service/delete-energiatodistus-luonnos! ts/*db* {:id laatija-id} id)))

(defn energiatodistus-tila [id] (-> id find-energiatodistus :tila-id service/tila-key))

(t/deftest start-energiatodistus-signing!-test
  (let [laatija-id (add-laatija!)
        whoami {:id laatija-id}
        id (add-energiatodistus! (generate-energiatodistus) laatija-id)]
    (t/is (= (energiatodistus-tila id) :draft))
    (t/is (= (service/start-energiatodistus-signing! ts/*db* whoami id) :ok))
    (t/is (= (energiatodistus-tila id) :in-signing))
    (t/is (= (service/start-energiatodistus-signing! ts/*db* whoami id) :already-in-signing))))

(t/deftest stop-energiatodistus-signing!-test
  (let [laatija-id (add-laatija!)
        whoami {:id laatija-id}
        id (add-energiatodistus! (generate-energiatodistus) laatija-id)]
    (t/is (= (energiatodistus-tila id) :draft))
    (t/is (=  (service/end-energiatodistus-signing! ts/*db* whoami id)
              :not-in-signing))
    (t/is (= (energiatodistus-tila id) :draft))
    (service/start-energiatodistus-signing! ts/*db* whoami id)
    (t/is (= (service/end-energiatodistus-signing! ts/*db* whoami id)
             :ok))
    (t/is (= (energiatodistus-tila id) :signed))
    (t/is (=  (service/end-energiatodistus-signing! ts/*db* whoami id)
              :already-signed))))
