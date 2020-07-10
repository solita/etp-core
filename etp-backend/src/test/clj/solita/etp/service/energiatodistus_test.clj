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
   schema/Rakennustunnus    (g/always "1035150826")
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
    (dissoc et :laatija-fullname :korvaava-energiatodistus-id)))

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
                 (dissoc :laatija-fullname :korvaava-energiatodistus-id))))
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

(defn add-energiatodistus-and-sign! [energiatodistus laatija-id]
  (let [id (add-energiatodistus! energiatodistus laatija-id)]
    (t/is (= (energiatodistus-tila id) :draft))
    (service/start-energiatodistus-signing! ts/*db* {:id laatija-id} id)
    (t/is (= (service/end-energiatodistus-signing! ts/*db* {:id laatija-id} id)
             :ok))
    (t/is (= (energiatodistus-tila id) :signed))
    id))

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
        update-energiatodistus   (assoc-in (generate-energiatodistus-2018) [:perustiedot :rakennustunnus] "103515074X")]
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

(t/deftest korvaa-energiatodistus!-test
  (let [laatija-id (add-laatija!)
        whoami {:id laatija-id}
        energiatodistus (generate-energiatodistus-2018)
        korvattava-energiatodistus-id (add-energiatodistus! energiatodistus laatija-id)]
    (t/is (= (energiatodistus-tila korvattava-energiatodistus-id) :draft))
    (service/start-energiatodistus-signing! ts/*db* whoami korvattava-energiatodistus-id)
    (t/is (= (service/end-energiatodistus-signing! ts/*db* whoami korvattava-energiatodistus-id)
             :ok))
    (t/is (= (energiatodistus-tila korvattava-energiatodistus-id) :signed))

    (let [korvaava-energiatodistus (assoc energiatodistus :korvattu-energiatodistus-id korvattava-energiatodistus-id)
          korvaava-energiatodistus-id (add-energiatodistus! korvaava-energiatodistus laatija-id)]
       (t/is (= (energiatodistus-tila korvaava-energiatodistus-id) :draft))
       (service/start-energiatodistus-signing! ts/*db* whoami korvaava-energiatodistus-id)
       (t/is (= (energiatodistus-tila korvattava-energiatodistus-id) :signed))
       (t/is (= (service/end-energiatodistus-signing! ts/*db* whoami korvaava-energiatodistus-id)
                :ok))
       (t/is (= (energiatodistus-tila korvaava-energiatodistus-id) :signed))
       (t/is (= (energiatodistus-tila korvattava-energiatodistus-id) :replaced)))))

(t/deftest korvaa-energiatodistus-states!-test
  (let [laatija-id                            (add-laatija!)
        whoami                                {:id laatija-id}
        energiatodistus                       (generate-energiatodistus-2018)
        signed-energiatodistus-id             (add-energiatodistus-and-sign! energiatodistus laatija-id)
        replaced-energiatodistus-id           (add-energiatodistus-and-sign! energiatodistus laatija-id)
        replaceses-energiatodistus-id         (add-energiatodistus-and-sign! (assoc energiatodistus :korvattu-energiatodistus-id replaced-energiatodistus-id) laatija-id)
        draft-energiatodistus-id              (add-energiatodistus! energiatodistus laatija-id)
        update-energiatodistus-id             (add-energiatodistus! energiatodistus laatija-id)
        first-replaceable-energiatodistus-id  (add-energiatodistus-and-sign! energiatodistus laatija-id)
        second-replaceable-energiatodistus-id (add-energiatodistus-and-sign! energiatodistus laatija-id)]

    ; find replaceable energiatodistukset
    (t/is (= (service/find-replaceable-energiatodistukset-like-id ts/*db* signed-energiatodistus-id) [signed-energiatodistus-id]))
    (t/is (= (service/find-replaceable-energiatodistukset-like-id ts/*db* replaced-energiatodistus-id) []))
    (t/is (= (service/find-replaceable-energiatodistukset-like-id ts/*db* replaceses-energiatodistus-id) [replaceses-energiatodistus-id]))
    (t/is (= (service/find-replaceable-energiatodistukset-like-id ts/*db* draft-energiatodistus-id) []))

    ; create energiatodistus with illegals and valid replaceable energiatodistus
    (t/is (thrown-with-msg? IllegalArgumentException #"Replaceable energiatodistus is not exists"
                            (add-energiatodistus! (assoc energiatodistus :korvattu-energiatodistus-id 101) laatija-id)))
    (t/is (thrown-with-msg? IllegalArgumentException #"Replaceable energiatodistus is not in signed or discarded state"
                            (add-energiatodistus! (assoc energiatodistus :korvattu-energiatodistus-id draft-energiatodistus-id) laatija-id)))
    (t/is (thrown-with-msg? IllegalArgumentException #"Replaceable energiatodistus is already replaced"
                            (add-energiatodistus! (assoc energiatodistus :korvattu-energiatodistus-id replaced-energiatodistus-id) laatija-id)))
    (t/is (number? (add-energiatodistus! (assoc energiatodistus :korvattu-energiatodistus-id first-replaceable-energiatodistus-id) laatija-id)))

    ; update energiatodistus with illegals and valid replaceable energiatodistus
    (t/is (thrown-with-msg? IllegalArgumentException #"Replaceable energiatodistus is not exists"
                           (service/update-energiatodistus-luonnos! ts/*db* whoami update-energiatodistus-id (assoc energiatodistus :korvattu-energiatodistus-id 101))))
    (t/is (thrown-with-msg? IllegalArgumentException #"Replaceable energiatodistus is not in signed or discarded state"
                            (service/update-energiatodistus-luonnos! ts/*db* whoami update-energiatodistus-id (assoc energiatodistus :korvattu-energiatodistus-id draft-energiatodistus-id))))
    (t/is (thrown-with-msg? IllegalArgumentException #"Replaceable energiatodistus is already replaced"
                            (service/update-energiatodistus-luonnos! ts/*db* whoami update-energiatodistus-id (assoc energiatodistus :korvattu-energiatodistus-id replaced-energiatodistus-id))))
    (t/is (= (service/update-energiatodistus-luonnos! ts/*db* whoami update-energiatodistus-id (assoc energiatodistus :korvattu-energiatodistus-id second-replaceable-energiatodistus-id)) 1))

    ; check states of energiatodistukset
    (t/is (= (energiatodistus-tila signed-energiatodistus-id) :signed))
    (t/is (= (energiatodistus-tila replaced-energiatodistus-id) :replaced))
    (t/is (= (energiatodistus-tila replaceses-energiatodistus-id) :signed))
    (t/is (= (energiatodistus-tila draft-energiatodistus-id) :draft))
    (t/is (= (energiatodistus-tila update-energiatodistus-id) :draft))
    (t/is (= (energiatodistus-tila first-replaceable-energiatodistus-id) :signed))
    (t/is (= (energiatodistus-tila second-replaceable-energiatodistus-id) :signed))))