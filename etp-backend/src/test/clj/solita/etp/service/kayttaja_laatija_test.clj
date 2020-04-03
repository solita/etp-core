(ns solita.etp.service.kayttaja-laatija-test
  (:require [clojure.test :as t]
            [schema.core :as schema]
            [schema-generators.generators :as g]
            [schema-tools.core :as st]
            [solita.etp.test-system :as ts]
            [solita.common.map :as map]
            [solita.etp.service.kayttaja-laatija :as service]
            [solita.etp.service.kayttaja :as kayttaja-service]
            [solita.etp.service.laatija :as laatija-service]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.geo :as geo-schema]
            [solita.etp.schema.kayttaja :as kayttaja-schema]
            [solita.etp.schema.laatija :as laatija-schema]
            [solita.etp.schema.kayttaja-laatija :as kayttaja-laatija-schema]))

(t/use-fixtures :each ts/fixture)

(def laatija {:rooli 0})
(def patevyydentoteaja {:rooli 1})
(def paakayttaja {:rooli 2})
(def roolit [laatija patevyydentoteaja paakayttaja])

(defn unique-henkilotunnus-range [to]
  (->> (range 0 to)
       (map (partial format "%09d"))
       (map #(str % (common-schema/henkilotunnus-checksum %)))
       (map #(str (subs % 0 6) "-" (subs % 6 10)))))

(def laatija-generators
  {common-schema/Henkilotunnus       (g/always "130200A892S")
   laatija-schema/MuutToimintaalueet (g/always [0, 1, 2, 3, 17])
   common-schema/Date                (g/always (java.time.LocalDate/now))
   geo-schema/Maa                    (g/always "FI")})

(defn generate-kayttaja-laatija [n schema]
  (map #(assoc %1 :henkilotunnus %2)
       (repeatedly n #(g/generate schema laatija-generators))
       (unique-henkilotunnus-range n)))

(defn generate-KayttajaLaatijaAdds [n]
  (generate-kayttaja-laatija n kayttaja-laatija-schema/KayttajaLaatijaAdd))

(defn generate-KayttajaLaatijaUpdates [n]
  (generate-kayttaja-laatija n kayttaja-laatija-schema/KayttajaLaatijaUpdate))

(t/deftest upsert-test
  (doseq [kayttaja-laatija (generate-KayttajaLaatijaAdds 100)
          :let [upsert-results (service/upsert-kayttaja-laatijat!
                                ts/*db*
                                [kayttaja-laatija])
                id (-> upsert-results first :kayttaja)
                found-kayttaja (kayttaja-service/find-kayttaja ts/*db* id)
                found-laatija (laatija-service/find-laatija-with-kayttaja-id
                               ts/*db*
                               id)]]
    (schema/validate kayttaja-schema/Kayttaja found-kayttaja)
    (schema/validate laatija-schema/Laatija found-laatija)
    (t/is (map/submap? (st/select-schema kayttaja-laatija
                                         kayttaja-schema/KayttajaAdd)
                       found-kayttaja))
    (t/is (map/submap? (st/select-schema kayttaja-laatija
                                         laatija-schema/LaatijaAdd)
                       found-laatija))))

(t/deftest upsert-existing-test
  (let [[original-1 original-2] (generate-KayttajaLaatijaAdds 2)
        original-1 (assoc original-1 :patevyystaso 0 :toteaja "FISE")
        _ (service/upsert-kayttaja-laatijat! ts/*db* [original-1 original-2])
        found-original-laatija-1 (laatija-service/find-laatija-with-henkilotunnus
                                  ts/*db*
                                  (:henkilotunnus original-1))
        found-original-laatija-2 (laatija-service/find-laatija-with-henkilotunnus
                                  ts/*db*
                                  (:henkilotunnus original-2))
        found-original-kayttaja-1 (kayttaja-service/find-kayttaja
                                   ts/*db*
                                   (:kayttaja found-original-laatija-1))
        found-original-kayttaja-2 (kayttaja-service/find-kayttaja
                                   ts/*db*
                                   (:kayttaja found-original-laatija-2))
        updated-1 (assoc original-1
                         :etunimi "Not updated"
                         :patevyystaso 1
                         :toteaja "KIINKO")
        _ (service/upsert-kayttaja-laatijat! ts/*db* [original-2 updated-1])
        found-updated-laatija-1 (laatija-service/find-laatija-with-henkilotunnus
                                  ts/*db*
                                  (:henkilotunnus original-1))
        found-updated-laatija-2 (laatija-service/find-laatija-with-henkilotunnus
                                  ts/*db*
                                  (:henkilotunnus original-2))
        found-updated-kayttaja-1 (kayttaja-service/find-kayttaja
                                   ts/*db*
                                   (:kayttaja found-original-laatija-1))
        found-updated-kayttaja-2 (kayttaja-service/find-kayttaja
                                   ts/*db*
                                   (:kayttaja found-original-laatija-2))]
    (schema/validate laatija-schema/Laatija found-original-laatija-1)
    (schema/validate laatija-schema/Laatija found-original-laatija-2)
    (schema/validate kayttaja-schema/Kayttaja found-original-kayttaja-1)
    (schema/validate kayttaja-schema/Kayttaja found-original-kayttaja-2)
    (schema/validate laatija-schema/Laatija found-updated-laatija-1)
    (schema/validate laatija-schema/Laatija found-updated-laatija-2)
    (schema/validate kayttaja-schema/Kayttaja found-updated-kayttaja-1)
    (schema/validate kayttaja-schema/Kayttaja found-updated-kayttaja-2)

    ;; Name (or the rest of the käyttäjä) is not be updated!
    (t/is (= found-original-kayttaja-1 found-updated-kayttaja-1))
    (t/is (= found-original-kayttaja-2 found-updated-kayttaja-2))

    ;; Laatija has been updated
    (t/is (not= found-original-laatija-1 found-updated-laatija-1))
    (t/is (zero? (:patevyystaso found-original-laatija-1)))
    (t/is (= (:patevyystaso found-updated-laatija-1) 1))
    (t/is (= (:toteaja found-original-laatija-1) "FISE"))
    (t/is (= (:toteaja found-updated-laatija-1) "KIINKO"))

    ;; The second laatija has not changed at all
    (t/is (= found-original-laatija-2 found-updated-laatija-2))))

(t/deftest update-kayttaja-laatija!-test
  (doseq [[add-1 update-1 add-2 update-2]
          (partition 4 (interleave (generate-KayttajaLaatijaAdds 100)
                                   (generate-KayttajaLaatijaUpdates 100)))
          :let [[id-1 id-2] (->> [add-1 add-2]
                                 (service/upsert-kayttaja-laatijat! ts/*db*)
                                 (map :kayttaja))
                kayttaja-1 {:id id-1}
                kayttaja-2 {:id id-2}
                whoami (rand-nth (concat [kayttaja-1 kayttaja-2] roolit))
                _ (service/update-kayttaja-laatija! ts/*db*
                                                    whoami
                                                    id-1
                                                    update-1)
                _ (service/update-kayttaja-laatija! ts/*db*
                                                    whoami
                                                    id-2
                                                    update-2)
                found-kayttaja-1 (kayttaja-service/find-kayttaja ts/*db* id-1)
                found-kayttaja-2 (kayttaja-service/find-kayttaja ts/*db* id-2)
                found-laatija-1 (laatija-service/find-laatija-with-kayttaja-id
                                 ts/*db*
                                 id-1)
                found-laatija-2 (laatija-service/find-laatija-with-kayttaja-id
                                 ts/*db*
                                 id-2)
                kayttaja-1-updated? (map/submap?
                                     (st/select-schema
                                      update-1
                                      kayttaja-schema/KayttajaUpdate)
                                     found-kayttaja-1)
                kayttaja-2-updated? (map/submap?
                                     (st/select-schema
                                      update-2
                                      kayttaja-schema/KayttajaUpdate)
                                     found-kayttaja-2)
                laatija-1-updated? (map/submap? (st/select-schema
                                                 update-1
                                                 laatija-schema/LaatijaUpdate)
                                                found-laatija-1)
                laatija-2-updated? (map/submap? (st/select-schema
                                                 update-2
                                                 laatija-schema/LaatijaUpdate)
                                                found-laatija-2)]]
    (schema/validate kayttaja-schema/Kayttaja found-kayttaja-1)
    (schema/validate kayttaja-schema/Kayttaja found-kayttaja-2)
    (schema/validate laatija-schema/Laatija found-laatija-1)
    (schema/validate laatija-schema/Laatija found-laatija-2)

    (cond
      (and (= whoami kayttaja-1)
           (every? #(not (contains? update-1 %))
                   service/ks-only-for-paakayttaja))
      (do
        (t/is (true? kayttaja-1-updated?))
        (t/is (true? laatija-1-updated?))
        (t/is (false? kayttaja-2-updated?))
        (t/is (false? laatija-2-updated?)))

      (and (= whoami kayttaja-2)
           (every? #(not (contains? update-1 %))
                   service/ks-only-for-paakayttaja))
      (do
        (t/is (false? kayttaja-1-updated?))
        (t/is (false? laatija-1-updated?))
        (t/is (true? kayttaja-2-updated?))
        (t/is (true? laatija-2-updated?)))

      (= whoami paakayttaja)
      (do
        (t/is (true? kayttaja-1-updated?))
        (t/is (true? laatija-1-updated?))
        (t/is (true? kayttaja-2-updated?))
        (t/is (true? laatija-2-updated?)))

      :else
      (do
        (t/is (false? kayttaja-1-updated?))
        (t/is (false? laatija-1-updated?))
        (t/is (false? kayttaja-2-updated?))
        (t/is (false? laatija-2-updated?))))))
