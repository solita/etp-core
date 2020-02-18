(ns solita.etp.service.laatija-test
  (:require [clojure.test :as t]
            [schema-generators.generators :as g]
            [solita.etp.test-system :as ts]
            [solita.etp.service.laatija :as service]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.laatija :as laatija-schema]
            [solita.etp.schema.laatija :as schema]))

(t/use-fixtures :each ts/fixture)

(def laatija-generators {common-schema/Hetu (g/always "130200A892S")
                         laatija-schema/MuutToimintaalueet
                         (g/always [0,1,2,3,17])})

(t/deftest add-and-find-test
  (doseq [laatija (repeatedly 100 #(g/generate schema/LaatijaSave laatija-generators))
          :let [id (service/add-laatija! ts/*db* laatija)]]
    (t/is (= (assoc laatija :id id) (service/find-laatija ts/*db* id)))))

(t/deftest find-patevyydet-test
  (t/is (= ["Perustaso" "Ylempi taso"] (->> (service/find-patevyydet)
                                             (map :label)))))
