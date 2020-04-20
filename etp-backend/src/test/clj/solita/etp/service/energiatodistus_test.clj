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
    (service/add-energiatodistus! ts/*db* laatija energiatodistus)))

(defn find-energiatodistus [id]
  (let [et (service/find-energiatodistus ts/*db* id)]
    (t/is (not (nil? (:laatija-id et))))
    (dissoc et :laatija-id)))

(t/deftest add-and-find-energiatodistus-test
  (doseq [energiatodistus (repeatedly 100 #(g/generate schema/EnergiatodistusSave2018 energiatodistus-generators))
          :let [id (add-energiatodistus! energiatodistus)]]
    (t/is (= (assoc energiatodistus :id id :tila "luonnos") (find-energiatodistus id))))
  (t/is (= 100 (count (service/find-all-luonnos-energiatodistukset ts/*db*))))
  (t/is (= 100 (count (service/find-all-energiatodistukset ts/*db*)))))

(t/deftest create-energiatodistus-and-set-valmis-test
  (let [id (add-energiatodistus! (g/generate schema/EnergiatodistusSave2018 energiatodistus-generators))]
    (t/is (= "luonnos" (:tila (find-energiatodistus id))))
    (t/is (= 1 (count (service/find-all-luonnos-energiatodistukset ts/*db*))))
    (t/is (= 1 (count (service/find-all-energiatodistukset ts/*db*))))
    (service/update-energiatodistus-as-valmis! ts/*db* id)
    (t/is (= "valmis" (:tila (find-energiatodistus id))))
    (t/is (= 0 (count (service/find-all-luonnos-energiatodistukset ts/*db*))))
    (t/is (= 1 (count (service/find-all-energiatodistukset ts/*db*))))
    (t/is (thrown? IllegalStateException (service/delete-energiatodistus-when-luonnos! ts/*db* id)))))

(t/deftest update-energiatodistus-test
  (let [id                     (add-energiatodistus! (g/generate schema/EnergiatodistusSave2018 energiatodistus-generators))
        update-energiatodistus (g/generate schema/EnergiatodistusSave2018 energiatodistus-generators)]
    (t/is (= "luonnos" (:tila (find-energiatodistus id))))
    (service/update-energiatodistus-when-luonnos! ts/*db* id update-energiatodistus)
    (t/is (= (assoc update-energiatodistus :id id :tila "luonnos") (find-energiatodistus id)))
    (service/update-energiatodistus-as-valmis! ts/*db* id)
    (t/is (thrown? IllegalStateException (service/update-energiatodistus-when-luonnos! ts/*db* id update-energiatodistus)))))

(t/deftest create-energiatodistus-and-delete-test
  (let [id (add-energiatodistus! (g/generate schema/EnergiatodistusSave2018 energiatodistus-generators))]
    (service/delete-energiatodistus-when-luonnos! ts/*db* id)))

