(ns solita.etp.service.kayttaja-test
  (:require [clojure.test :as t]
            [schema.core :as schema]
            [schema-generators.generators :as g]
            [solita.common.map :as map]
            [solita.etp.test-system :as ts]
            [solita.etp.service.kayttaja :as service]
            [solita.etp.schema.kayttaja :as kayttaja-schema]))

(t/use-fixtures :each ts/fixture)

(def laatija {:rooli 0})
(def patevyydentoteaja {:rooli 1})
(def paakayttaja {:rooli 2})
(def roolit [laatija patevyydentoteaja paakayttaja])

(t/deftest add-and-find-test
  (doseq [kayttaja (repeatedly 100 #(g/generate kayttaja-schema/KayttajaAdd))
          :let [id (service/add-kayttaja! ts/*db* kayttaja)
                whoami (rand-nth (conj roolit {:id id}))
                found (service/find-kayttaja ts/*db* whoami id)]]
    (if (= whoami laatija)
      (t/is (nil? found))
      (do
        (schema/validate kayttaja-schema/Kayttaja found)
        (t/is (map/submap? kayttaja found))))))

(defn assoc-idx-email [idx kayttaja]
  (assoc kayttaja :email (str "kayttaja" idx "@example.com")))

(t/deftest add-update-and-find-test
  (doseq [kayttaja (repeatedly 100 #(g/generate kayttaja-schema/KayttajaAdd))
          :let [id (service/add-kayttaja! ts/*db* kayttaja)
                updated-kayttaja (g/generate kayttaja-schema/KayttajaUpdate)
                whoami (rand-nth (conj roolit {:id id}))
                _ (service/update-kayttaja! ts/*db*
                                            whoami
                                            id
                                            updated-kayttaja)
                found (service/find-kayttaja ts/*db* paakayttaja id)]]
    (schema/validate kayttaja-schema/Kayttaja found)
    (if (contains? #{laatija patevyydentoteaja} whoami)
      (t/is (map/submap? kayttaja found))
      (t/is (map/submap? updated-kayttaja found)))))

(t/deftest update-login!-test
  (doseq [kayttaja (repeatedly 100 #(g/generate kayttaja-schema/KayttajaAdd))
          :let [id (service/add-kayttaja! ts/*db* kayttaja)
                found-before-login (service/find-kayttaja ts/*db* paakayttaja id)
                cognitoid (str "cognitoid-" (rand-int 1000000))
                _ (service/update-login! ts/*db* id cognitoid)
                found-after-login (service/find-kayttaja ts/*db* paakayttaja id)]]
    (schema/validate kayttaja-schema/Kayttaja found-after-login)
    (t/is (-> found-before-login :login nil?))
    (t/is (-> found-after-login :login nil? not))
    (t/is (-> found-before-login :cognitoid nil?))
    (t/is (= cognitoid (:cognitoid found-after-login)))))
