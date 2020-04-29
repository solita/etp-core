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
      (get 0)
      :laatija))

(defn add-energiatodistus! [energiatodistus]
  (let [laatija {:laatija (add-laatija!)}]
    (service/add-energiatodistus! ts/*db* laatija 2018 energiatodistus)))

(defn find-energiatodistus [id]
  (let [et (service/find-energiatodistus ts/*db* id)]
    (t/is (not (nil? (:laatija-fullname et))))
    (dissoc et :laatija-fullname)))

(defn merge-energiatodistus-defaults [energiatodistus id]
  (merge energiatodistus
         {:id id
          :versio 2018
          :allekirjoituksessaaika nil
          :allekirjoitusaika nil}))

(t/deftest add-and-find-energiatodistus-test
  (doseq [energiatodistus (repeatedly 100 #(g/generate schema/EnergiatodistusSave2018 energiatodistus-generators))
          :let [id (add-energiatodistus! energiatodistus)]]
    (t/is (= (merge-energiatodistus-defaults energiatodistus id) (find-energiatodistus id)))))

(t/deftest update-energiatodistus-test
  (let [id                     (add-energiatodistus! (g/generate schema/EnergiatodistusSave2018 energiatodistus-generators))
        update-energiatodistus (g/generate schema/EnergiatodistusSave2018 energiatodistus-generators)]

    (service/update-energiatodistus-luonnos! ts/*db* id update-energiatodistus)
    (t/is (= (merge-energiatodistus-defaults update-energiatodistus id)
             (find-energiatodistus id)))))

(t/deftest create-energiatodistus-and-delete-test
  (let [id (add-energiatodistus! (g/generate schema/EnergiatodistusSave2018 energiatodistus-generators))]
    (service/delete-energiatodistus-luonnos! ts/*db* id)))

(t/deftest start-energiatodistus-signing!-test
  (let [id (add-energiatodistus! (g/generate schema/EnergiatodistusSave2018 energiatodistus-generators))
        found-before-update (find-energiatodistus id)
        start-signing-result-1 (service/start-energiatodistus-signing! ts/*db* id)
        found-after-update (find-energiatodistus id)
        start-signing-result-2 (service/start-energiatodistus-signing! ts/*db* id)]
    (t/is (-> found-before-update :allekirjoituksessaaika nil?))
    (t/is (= start-signing-result-1 :ok))
    (t/is (-> found-after-update :allekirjoituksessaaika nil? not))
    (t/is (= start-signing-result-2 :already-in-signing))))

(t/deftest stop-energiatodistus-signing!-test
  (let [id (add-energiatodistus! (g/generate schema/EnergiatodistusSave2018 energiatodistus-generators))
        found-before-update-1 (find-energiatodistus id)
        stop-signing-result-1 (service/stop-energiatodistus-signing! ts/*db* id)
        _ (service/start-energiatodistus-signing! ts/*db* id)
        found-before-update-2 (find-energiatodistus id)
        stop-signing-result-2 (service/stop-energiatodistus-signing! ts/*db* id)
        found-after-update (find-energiatodistus id)
        stop-signing-result-3 (service/stop-energiatodistus-signing! ts/*db* id)]
    (t/is (-> found-before-update-1 :allekirjoitusaika nil?))
    (t/is (= stop-signing-result-1 :not-in-signing))
    (t/is (-> found-before-update-2 :allekirjoitusaika nil?))
    (t/is (= stop-signing-result-2 :ok))
    (t/is (-> found-after-update :allekirjoitusaika nil? not))
    (t/is (= stop-signing-result-3 :already-signed))))
