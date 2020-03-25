(ns solita.etp.service.kayttaja-test
  (:require [clojure.test :as t]
            [schema.core :as schema]
            [schema-generators.generators :as g]
            [solita.common.map :as map]
            [solita.etp.test-system :as ts]
            [solita.etp.service.kayttaja :as service]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.kayttaja :as kayttaja-schema]))

(t/use-fixtures :each ts/fixture)

(t/deftest add-and-find-test
  (doseq [kayttaja (repeatedly 100 #(g/generate kayttaja-schema/KayttajaAdd))
          :let [id (service/add-kayttaja! ts/*db* kayttaja)
                found (service/find-kayttaja ts/*db* id)]]
    (schema/validate kayttaja-schema/Kayttaja found)
    (t/is (map/submap? kayttaja found))))

(t/deftest add-update-and-find-test
  (doseq [kayttaja (repeatedly 100 #(g/generate kayttaja-schema/KayttajaAdd))
          :let [id (service/add-kayttaja! ts/*db* kayttaja)
                found (service/find-kayttaja ts/*db* id)
                updated-kayttaja (g/generate kayttaja-schema/KayttajaUpdate)
                _ (service/update-kayttaja! ts/*db* id updated-kayttaja)
                found (service/find-kayttaja ts/*db* id)]]
    (schema/validate kayttaja-schema/Kayttaja found)
    (t/is (map/submap? updated-kayttaja found))))

(t/deftest find-roolit-test
  (let [roolit (service/find-roolit)
        fi-labels (set (map :label-fi roolit))]
    (t/is (= fi-labels #{"Laatija"
                         "Pätevyyden toteaja"
                         "Pääkäyttäjä"}))
    ;; TODO test swedish labels when they exist
    ))
