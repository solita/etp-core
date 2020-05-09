(ns solita.etp.service.energiatodistus-test
  (:require [clojure.test :as t]
            [schema-generators.generators :as g]
            [solita.etp.test-system :as ts]
            [solita.etp.service.energiatodistus :as service]
            [solita.etp.service.kayttaja-laatija :as laatija-service]
            [solita.etp.service.kayttaja-laatija-test :as laatija-service-test]
            [solita.etp.schema.energiatodistus :as schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.geo :as geo-schema]))

(t/use-fixtures :each ts/fixture)

(def energiatodistus-generators
  {schema.core/Num          (g/always 1.0)
   schema/Rakennustunnus    (g/always "1234567890")
   schema/YritysPostinumero (g/always "00100")
   common-schema/Date       (g/always (java.time.LocalDate/now))
   common-schema/Integer100 (g/always 50)
   geo-schema/Postinumero   (g/always "00100")})

(defn add-laatija! []
  (-> (laatija-service/upsert-kayttaja-laatijat! ts/*db*
        (laatija-service-test/generate-KayttajaLaatijaAdds 1))
      first))

(defn add-energiatodistus! [energiatodistus laatija-id]
  (service/add-energiatodistus! ts/*db* {:laatija laatija-id} 2018 energiatodistus))

(defn find-energiatodistus [id]
  (let [et (service/find-energiatodistus ts/*db* id)]
    (t/is (not (nil? (:laatija-fullname et))))
    (dissoc et :laatija-fullname)))

(defn complete-energiatodistus [energiatodistus id laatija-id]
  (merge energiatodistus
         {:id id
          :laatija-id laatija-id
          :versio 2018
          :allekirjoituksessaaika nil
          :allekirjoitusaika nil}))

(t/deftest add-and-find-energiatodistus-test
  (let [laatija-id (add-laatija!)]
    (doseq [energiatodistus (repeatedly 100 #(g/generate schema/EnergiatodistusSave2018
                                                         energiatodistus-generators))
            :let [id (add-energiatodistus! energiatodistus laatija-id)]]
      (t/is (= (complete-energiatodistus energiatodistus id laatija-id)
               (find-energiatodistus id))))))

(t/deftest permissions-test
  (let [patevyydentoteaja {:rooli 1}
        paakayttaja {:rooli 2}
        laatija-id (add-laatija!)
        energiatodistus (g/generate schema/EnergiatodistusSave2018 energiatodistus-generators)
        id (add-energiatodistus! energiatodistus laatija-id)]
    (t/is (= (complete-energiatodistus energiatodistus id laatija-id)
             (-> (service/find-energiatodistus ts/*db* paakayttaja id)
                 (dissoc :laatija-fullname))))
    (t/is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Forbidden"
                            (service/find-energiatodistus ts/*db* patevyydentoteaja id)))))

(t/deftest update-energiatodistus-test
  (let [laatija-id (add-laatija!)
        id (add-energiatodistus! (g/generate schema/EnergiatodistusSave2018 energiatodistus-generators) laatija-id)
        update-energiatodistus (g/generate schema/EnergiatodistusSave2018 energiatodistus-generators)]
    (service/update-energiatodistus-luonnos! ts/*db* {:laatija laatija-id} id update-energiatodistus)
    (t/is (= (complete-energiatodistus update-energiatodistus id laatija-id)
             (find-energiatodistus id)))))

(t/deftest create-energiatodistus-and-delete-test
  (let [laatija-id (add-laatija!)
        id (add-energiatodistus! (g/generate schema/EnergiatodistusSave2018 energiatodistus-generators) laatija-id)]
    (service/delete-energiatodistus-luonnos! ts/*db* {:laatija laatija-id} id)))

(t/deftest start-energiatodistus-signing!-test
  (let [laatija-id (add-laatija!)
        id (add-energiatodistus! (g/generate schema/EnergiatodistusSave2018 energiatodistus-generators) laatija-id)]
    (t/is (-> (find-energiatodistus id) :allekirjoituksessaaika nil?))
    (t/is (= (service/start-energiatodistus-signing! ts/*db* id) :ok))
    (t/is (-> (find-energiatodistus id) :allekirjoituksessaaika nil? not))
    (t/is (= (service/start-energiatodistus-signing! ts/*db* id) :already-in-signing))))

(t/deftest stop-energiatodistus-signing!-test
  (let [laatija-id (add-laatija!)
        id (add-energiatodistus! (g/generate schema/EnergiatodistusSave2018 energiatodistus-generators) laatija-id)]
    (t/is (-> (find-energiatodistus id) :allekirjoitusaika nil?))
    (t/is (=  (service/stop-energiatodistus-signing! ts/*db* id)
              :not-in-signing))
    (t/is (-> (find-energiatodistus id) :allekirjoitusaika nil?))
    (service/start-energiatodistus-signing! ts/*db* id)
    (t/is (= (service/stop-energiatodistus-signing! ts/*db* id)
             :ok))
    (t/is (-> (find-energiatodistus id) :allekirjoitusaika nil? not))
    (t/is (=  (service/stop-energiatodistus-signing! ts/*db* id)
              :already-signed))))
