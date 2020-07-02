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
   (add-energiatodistus! energiatodistus laatija-id 2018))
  ([energiatodistus laatija-id versio]
    (service/add-energiatodistus! (ts/db-user laatija-id) {:id laatija-id} versio energiatodistus)))

(defn find-energiatodistus [id]
  (let [et (service/find-energiatodistus ts/*db* id)]
    (t/is (not (nil? (:laatija-fullname et))))
    (dissoc et :laatija-fullname)))

(defn complete-energiatodistus
  ([energiatodistus id laatija-id] (complete-energiatodistus energiatodistus id laatija-id 2018))
  ([energiatodistus id laatija-id versio]
    (merge energiatodistus
           {:id id
            :laatija-id laatija-id
            :versio versio
            :tila-id 0
            :allekirjoitusaika nil})))

(defn generate-energiatodistus-2018 []
  (-> (g/generate schema/EnergiatodistusSave2018
                  energiatodistus-generators)
      ;; fix fk references in generated content
      (assoc :korvattu-energiatodistus-id nil)
      (assoc-in [:perustiedot :kayttotarkoitus] "YAT")))

(defn generate-energiatodistus-2013 []
  (-> (g/generate schema/EnergiatodistusSave2013
                  energiatodistus-generators)
      ;; fix fk references in generated content
      (assoc :korvattu-energiatodistus-id nil)
      (assoc-in [:perustiedot :kayttotarkoitus] "YAT")))

(defn test-add-and-find-energiatodistus [versio generation]
  (let [laatija-id (add-laatija!)]
    (doseq [energiatodistus (repeatedly 100 generation)
            :let [id (add-energiatodistus! energiatodistus laatija-id versio)]]
      (t/is (= (complete-energiatodistus energiatodistus id laatija-id versio)
               (find-energiatodistus id))))))

(t/deftest add-and-find-energiatodistus-2018-test
  (test-add-and-find-energiatodistus 2018 generate-energiatodistus-2018))

(t/deftest add-and-find-energiatodistus-2013-test
  (test-add-and-find-energiatodistus 2013 generate-energiatodistus-2013))

(t/deftest permissions-test
  (let [patevyydentoteaja {:rooli 1}
        paakayttaja {:rooli 2}
        laatija-id (add-laatija!)
        energiatodistus (generate-energiatodistus-2018)
        id (add-energiatodistus! energiatodistus laatija-id)]
    (t/is (= (complete-energiatodistus energiatodistus id laatija-id)
             (-> (service/find-energiatodistus ts/*db* paakayttaja id)
                 (dissoc :laatija-fullname))))
    (t/is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Forbidden"
                            (service/find-energiatodistus ts/*db* patevyydentoteaja id)))))

(t/deftest update-energiatodistus-test
  (let [laatija-id (add-laatija!)
        id (add-energiatodistus! (generate-energiatodistus-2018) laatija-id)
        update-energiatodistus (generate-energiatodistus-2018)]
    (service/update-energiatodistus-luonnos! ts/*db* {:id laatija-id} id update-energiatodistus)
    (t/is (= (complete-energiatodistus update-energiatodistus id laatija-id)
             (find-energiatodistus id)))))

(t/deftest create-energiatodistus-and-delete-test
  (let [laatija-id (add-laatija!)
        id (add-energiatodistus! (generate-energiatodistus-2018) laatija-id)]
    (service/delete-energiatodistus-luonnos! ts/*db* {:id laatija-id} id)))

(defn energiatodistus-tila [id] (-> id find-energiatodistus :tila-id service/tila-key))

(t/deftest start-energiatodistus-signing!-test
  (let [laatija-id (add-laatija!)
        whoami {:id laatija-id}
        id (add-energiatodistus! (generate-energiatodistus-2018) laatija-id)]
    (t/is (= (energiatodistus-tila id) :draft))
    (t/is (= (service/start-energiatodistus-signing! ts/*db* whoami id) :ok))
    (t/is (= (energiatodistus-tila id) :in-signing))
    (t/is (= (service/start-energiatodistus-signing! ts/*db* whoami id) :already-in-signing))))

(t/deftest stop-energiatodistus-signing!-test
  (let [laatija-id (add-laatija!)
        whoami {:id laatija-id}
        id (add-energiatodistus! (generate-energiatodistus-2018) laatija-id)]
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

(t/deftest update-signed-energiatodistus!-test
  (let [laatija-id               (add-laatija!)
        whoami                   {:id laatija-id}
        original-energiatodistus (generate-energiatodistus-2018)
        id                       (add-energiatodistus! original-energiatodistus laatija-id)
        update-energiatodistus   (assoc-in (generate-energiatodistus-2018) [:perustiedot :rakennustunnus] "4444444444")]
    (t/is (= (energiatodistus-tila id) :draft))
    (service/start-energiatodistus-signing! ts/*db* whoami id)
    (t/is (= (service/end-energiatodistus-signing! ts/*db* whoami id)
             :ok))
    (t/is (= (energiatodistus-tila id) :signed))
    (t/is (= 1 (service/update-energiatodistus-luonnos! ts/*db* {:id laatija-id} id update-energiatodistus)))
    (let [energiatodistus (find-energiatodistus id)]
      (t/is (= (-> (complete-energiatodistus original-energiatodistus id laatija-id)
                   (assoc-in [:perustiedot :rakennustunnus] (-> update-energiatodistus :perustiedot :rakennustunnus))
                   (assoc :tila-id 2
                          :allekirjoitusaika (:allekirjoitusaika energiatodistus)))
               energiatodistus)))))