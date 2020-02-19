(ns solita.etp.service.yritys-test
  (:require [clojure.test :as t]
            [schema-generators.generators :as g]
            [solita.etp.test-system :as ts]
            [solita.etp.service.yritys :as service]
            [solita.etp.schema.yritys :as schema]))

(t/use-fixtures :each ts/fixture)

(t/deftest add-and-find-yritys-test
  (doseq [yritys (repeatedly 100 #(g/generate schema/YritysSave))
          :let [id (service/add-yritys! ts/*db* yritys)]]
    (t/is (= (assoc yritys :id id) (service/find-yritys ts/*db* id)))))

