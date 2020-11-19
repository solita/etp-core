(ns solita.etp.service.kayttaja-laatija-test
  (:require [clojure.test :as t]
            [schema.core :as schema]
            [schema-generators.generators :as g]
            [schema-tools.core :as st]
            [solita.etp.test-system :as ts]
            [solita.etp.test-utils :as tu]
            [solita.common.map :as map]
            [solita.etp.service.kayttaja-laatija :as service]
            [solita.etp.service.kayttaja :as kayttaja-service]
            [solita.etp.service.laatija :as laatija-service]
            [solita.etp.schema.kayttaja :as kayttaja-schema]
            [solita.etp.schema.laatija :as laatija-schema]))

(t/use-fixtures :each ts/fixture)

(def laatija {:rooli 0})
(def patevyydentoteaja {:rooli 1})
(def paakayttaja {:rooli 2})
(def roolit [laatija patevyydentoteaja paakayttaja])

(def LaatijaUpdate (dissoc laatija-schema/LaatijaUpdate :api-key))

(defn generate-KayttajaLaatijaAdds [n]
  (tu/generate-kayttaja n laatija-schema/KayttajaLaatijaAdd))

(defn generate-KayttajaLaatijaUpdates [n]
  (->> laatija-schema/KayttajaLaatijaUpdate
       (tu/generate-kayttaja n)
       (map #(assoc % :rooli 0))))

(t/deftest upsert-test
  (doseq [kayttaja-laatija (generate-KayttajaLaatijaAdds 100)
          :let [upsert-results (service/upsert-kayttaja-laatijat!
                                ts/*db*
                                [kayttaja-laatija])
                id (-> upsert-results first)
                found-kayttaja (kayttaja-service/find-kayttaja ts/*db* id)
                found-laatija (laatija-service/find-laatija-by-id
                               ts/*db*
                               id)]]
    (schema/validate kayttaja-schema/Kayttaja found-kayttaja)
    (schema/validate laatija-schema/Laatija found-laatija)
    (t/is (map/submap? (st/select-schema kayttaja-laatija
                                         laatija-schema/KayttajaAdd)
                       found-kayttaja))
    (t/is (map/submap? (st/select-schema kayttaja-laatija
                                         laatija-schema/LaatijaAdd)
                       found-laatija))))

(def Laatija (dissoc laatija-schema/Laatija
                     :voimassa :voimassaolo-paattymisaika))

(t/deftest upsert-existing-test
  (let [[original-1 original-2] (generate-KayttajaLaatijaAdds 2)

        ;; Add schema is used when upserting käyttäjät with laatijat
        [update-1] (generate-KayttajaLaatijaAdds 1)
        _ (service/upsert-kayttaja-laatijat! ts/*db* [original-1 original-2])
        found-original-laatija-1 (laatija-service/find-laatija-with-henkilotunnus
                                  ts/*db*
                                  (:henkilotunnus original-1))
        found-original-laatija-2 (laatija-service/find-laatija-with-henkilotunnus
                                  ts/*db*
                                  (:henkilotunnus original-2))
        found-original-kayttaja-1 (kayttaja-service/find-kayttaja
                                   ts/*db*
                                   (:id found-original-laatija-1))
        found-original-kayttaja-2 (kayttaja-service/find-kayttaja
                                   ts/*db*
                                   (:id found-original-laatija-2))
        _ (service/upsert-kayttaja-laatijat! ts/*db* [original-2 update-1])
        found-updated-laatija-1 (laatija-service/find-laatija-with-henkilotunnus
                                  ts/*db*
                                  (:henkilotunnus original-1))
        found-updated-laatija-2 (laatija-service/find-laatija-with-henkilotunnus
                                  ts/*db*
                                  (:henkilotunnus original-2))
        found-updated-kayttaja-1 (kayttaja-service/find-kayttaja
                                   ts/*db*
                                   (:id found-original-laatija-1))
        found-updated-kayttaja-2 (kayttaja-service/find-kayttaja
                                   ts/*db*
                                   (:id found-original-laatija-2))]
    (schema/validate Laatija found-original-laatija-1)
    (schema/validate Laatija found-original-laatija-2)
    (schema/validate kayttaja-schema/Kayttaja found-original-kayttaja-1)
    (schema/validate kayttaja-schema/Kayttaja found-original-kayttaja-2)
    (schema/validate Laatija found-updated-laatija-1)
    (schema/validate Laatija found-updated-laatija-2)
    (schema/validate kayttaja-schema/Kayttaja found-updated-kayttaja-1)
    (schema/validate kayttaja-schema/Kayttaja found-updated-kayttaja-2)

    ;; The first käyttäjä and laatija has been updated
    (t/is (not= found-original-kayttaja-1 found-updated-kayttaja-1))
    (t/is (not= found-original-laatija-1 found-updated-laatija-1))
    (t/is (-> update-1
              (st/select-keys laatija-schema/KayttajaAdd)
              (map/submap? found-updated-kayttaja-1)))
    (t/is (-> update-1
              (st/select-keys laatija-schema/LaatijaAdd)
              (map/submap? found-updated-laatija-1)))

    ;; The second käyttäjä and laatija has not changed at all
    (t/is (= found-original-kayttaja-2 found-updated-kayttaja-2))
    (t/is (= found-original-laatija-2 found-updated-laatija-2))))

(defn update-kayttaja-laatija! [whoami id kayttaja-laatija]
  (try
    (service/update-kayttaja-laatija! ts/*db* whoami id kayttaja-laatija)
    (catch Exception e (if (not= (-> e ex-data :type) :forbidden)
                         (throw e)))))

(t/deftest update-kayttaja-laatija!-test
  (doseq [[add-1 update-1 add-2 update-2]
          (partition 4 (interleave (generate-KayttajaLaatijaAdds 100)
                                   (generate-KayttajaLaatijaUpdates 100)))
          :let [[id-1 id-2] (->> [add-1 add-2]
                                 (service/upsert-kayttaja-laatijat! ts/*db*))
                kayttaja-1 {:id id-1}
                kayttaja-2 {:id id-2}
                whoami (rand-nth (concat [kayttaja-1 kayttaja-2] roolit))
                _ (update-kayttaja-laatija! whoami id-1 update-1)
                _ (update-kayttaja-laatija! whoami id-2 update-2)
                found-kayttaja-1 (kayttaja-service/find-kayttaja ts/*db* id-1)
                found-kayttaja-2 (kayttaja-service/find-kayttaja ts/*db* id-2)
                found-laatija-1 (laatija-service/find-laatija-by-id
                                 ts/*db*
                                 id-1)
                found-laatija-2 (laatija-service/find-laatija-by-id
                                 ts/*db*
                                 id-2)
                kayttaja-1-updated? (map/submap?
                                     (st/select-schema
                                      update-1
                                      laatija-schema/KayttajaUpdate)
                                     found-kayttaja-1)
                kayttaja-2-updated? (map/submap?
                                     (st/select-schema
                                      update-2
                                      laatija-schema/KayttajaUpdate)
                                     found-kayttaja-2)
                laatija-1-updated? (map/submap? (st/select-schema
                                                 update-1
                                                 LaatijaUpdate)
                                                found-laatija-1)
                laatija-2-updated? (map/submap? (st/select-schema
                                                 update-2
                                                 LaatijaUpdate)
                                                found-laatija-2)]]
    (schema/validate kayttaja-schema/Kayttaja found-kayttaja-1)
    (schema/validate kayttaja-schema/Kayttaja found-kayttaja-2)
    (schema/validate laatija-schema/Laatija found-laatija-1)
    (schema/validate laatija-schema/Laatija found-laatija-2)

    (cond
      (and (= whoami kayttaja-1)
           (every? #(not (contains? update-1 %))
                   (keys laatija-schema/LaatijaAdminUpdate))
           (every? #(not (contains? update-1 %))
                   (keys laatija-schema/KayttajaAdminUpdate)))
      (do
        (t/is (true? kayttaja-1-updated?))
        (t/is (true? laatija-1-updated?))
        (t/is (false? kayttaja-2-updated?))
        (t/is (false? laatija-2-updated?)))

      (and (= whoami kayttaja-2)
           (every? #(not (contains? update-2 %))
                   (keys laatija-schema/LaatijaAdminUpdate))
           (every? #(not (contains? update-2 %))
                   (keys laatija-schema/KayttajaAdminUpdate)))
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
