(ns solita.etp.service.kayttaja-test
  (:require [clojure.test :as t]
            [schema.core :as schema]
            [schema-generators.generators :as g]
            [solita.common.map :as map]
            [solita.etp.test-system :as ts]
            [solita.etp.service.kayttaja :as service]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.schema.kayttaja :as kayttaja-schema]
            [solita.etp.schema.laatija :as laatija-schema]
            [solita.etp.schema.common :as common-schema]))

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
  (doseq [kayttaja (repeatedly 100 #(g/generate laatija-schema/KayttajaAdd))
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

(defn update-kayttaja! [whoami id kayttaja]
  (try
    (service/update-kayttaja! ts/*db* whoami id kayttaja)
    (catch Exception e (if (not= (-> e ex-data :type) :forbidden)
                         (throw e)))))

(t/deftest add-update-and-find-test
  (doseq [kayttaja (repeatedly 100 #(g/generate kayttaja-schema/KayttajaAdd))
          :let [id (service/add-kayttaja! ts/*db* kayttaja)
                updated-kayttaja (g/generate kayttaja-schema/KayttajaUpdate)
                whoami (rand-nth (conj roolit {:id id}))
                _ (update-kayttaja! whoami id updated-kayttaja)
                new-rooli (:rooli updated-kayttaja)
                found (find-kayttaja whoami id)]]
    (cond
      ;; If whoami has no permission to find the käyttäjä
      (not (or (= (:id whoami) id)
               (= whoami paakayttaja)
               (and (= whoami patevyydentoteaja)
                    (rooli-service/laatija? found))))
      (t/is (nil? found))

      ;; If whoami has no permission to update the käyttäjä
      (not (or (and (= (:id whoami) id)
                    (common-schema/not-contains-keys
                      updated-kayttaja
                      kayttaja-schema/KayttajaAdminUpdate))
               (= whoami paakayttaja)))
      (do (schema/validate kayttaja-schema/Kayttaja found)
          (t/is (map/submap? kayttaja found)))

      ;; Otherwise the update was a success
      :else
      (do (schema/validate kayttaja-schema/Kayttaja found)
          (t/is (map/submap? updated-kayttaja found))))))
