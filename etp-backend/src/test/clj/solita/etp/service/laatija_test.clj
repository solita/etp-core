(ns solita.etp.service.laatija-test
  (:require [clojure.test :as t]
            [schema-generators.generators :as g]
            [solita.etp.test-system :as ts]
            [solita.etp.service.laatija :as service]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.laatija :as schema]))

(t/use-fixtures :each ts/fixture)

(def hetu-generator {common-schema/Hetu (g/always "130200A892S")})

(t/deftest add-and-find-test
  (doseq [laatija (repeatedly 100 #(g/generate schema/LaatijaSave hetu-generator))
          :let [id (service/add-laatija! ts/*db* laatija)]]
    (t/is (= (assoc laatija :id id) (service/find-laatija ts/*db* id)))))
