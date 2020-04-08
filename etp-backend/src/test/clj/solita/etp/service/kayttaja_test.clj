(ns solita.etp.service.kayttaja-test
  (:require [clojure.test :as t]
            [schema.core :as schema]
            [schema-generators.generators :as g]
            [solita.common.map :as map]
            [solita.etp.test-system :as ts]
            [solita.etp.service.kayttaja :as service]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.schema.kayttaja :as kayttaja-schema]))

(t/use-fixtures :each ts/fixture)

(def laatija {:rooli 0})
(def patevyydentoteaja {:rooli 1})
(def paakayttaja {:rooli 2})
(def roolit [laatija patevyydentoteaja paakayttaja])

(defn find-kayttaja [whoami id]
  (try
    (service/find-kayttaja ts/*db* whoami id)
    (catch Exception e (if (not= (-> e ex-data :type) :forbidden)
                         (throw e)))))

(t/deftest add-and-find-test
  (doseq [kayttaja (repeatedly 100 #(g/generate kayttaja-schema/KayttajaAdd))
          :let [id (service/add-kayttaja! ts/*db* kayttaja)
                whoami (rand-nth (conj roolit {:id id}))
                found (find-kayttaja whoami id)]]
    (if (= whoami laatija)
      (t/is (nil? found))
      (do
        (schema/validate kayttaja-schema/Kayttaja found)
        (t/is (map/submap? kayttaja found))))))

(defn assoc-idx-email [idx kayttaja]
  (assoc kayttaja :email (str "kayttaja" idx "@example.com")))

(t/deftest allow-rooli-update?-test
  (t/is (true? (service/allow-rooli-update? 0 nil)))
  (t/is (false? (service/allow-rooli-update? 0 1)))
  (t/is (false? (service/allow-rooli-update? 0 2)))
  (t/is (false? (service/allow-rooli-update? 1 0)))
  (t/is (true? (service/allow-rooli-update? 1 1)))
  (t/is (true? (service/allow-rooli-update? 1 2)))
  (t/is (false? (service/allow-rooli-update? 2 0)))
  (t/is (true? (service/allow-rooli-update? 2 1)))
  (t/is (true? (service/allow-rooli-update? 2 2))))

(defn update-kayttaja! [id kayttaja]
  (try
    (service/update-kayttaja! ts/*db* id kayttaja)
    (catch Exception e (if (not= (-> e ex-data :type) :forbidden)
                         (throw e)))))

(t/deftest add-update-and-find-test
  (doseq [kayttaja (repeatedly 100 #(g/generate kayttaja-schema/KayttajaAdd))
          :let [id (service/add-kayttaja! ts/*db* kayttaja)
                updated-kayttaja (g/generate kayttaja-schema/KayttajaUpdate)
                _ (update-kayttaja! id updated-kayttaja)
                new-rooli (:rooli updated-kayttaja)
                whoami (rand-nth (conj roolit {:id id}))
                found (find-kayttaja whoami id)]]
    (cond
      ;; If whoami had no permission to find the käyttäjä
      (or (= whoami laatija)
          (and (= whoami patevyydentoteaja)
               (rooli-service/laatija-maintainer? found)))
      (t/is (nil? found))

      ;; If changing the rooli was attempted
      (and (-> new-rooli nil? not)
           (-> new-rooli zero? not))
      (do (schema/validate kayttaja-schema/Kayttaja found)
          (t/is (map/submap? kayttaja found)))

      ;; Otherwise the update was a success
      :else
      (do (schema/validate kayttaja-schema/Kayttaja found)
          (t/is (map/submap? updated-kayttaja found))))))

(t/deftest update-kayttaja-with-whoami!-test
  (doseq [kayttaja (repeatedly 100 #(g/generate kayttaja-schema/KayttajaAdd))
          :let [id (service/add-kayttaja! ts/*db* kayttaja)
                found-before (service/find-kayttaja ts/*db* paakayttaja id)
                new-email (str "new-" (:email found-before))
                cognitoid (str "cognitoid-" (rand-int 1000000))
                _ (service/update-kayttaja-with-whoami! ts/*db*
                                                        {:id id
                                                         :email new-email
                                                         :cognitoid cognitoid})
                found-after (service/find-kayttaja ts/*db* paakayttaja id)]]
    (schema/validate kayttaja-schema/Kayttaja found-after)
    (t/is (-> found-before :login nil?))
    (t/is (-> found-after :login nil? not))
    (t/is (-> found-after :email (= new-email)))
    (t/is (-> found-before :cognitoid nil?))
    (t/is (= cognitoid (:cognitoid found-after)))))
