(ns solita.etp.service.yritys-test
  (:require [clojure.test :as t]
            [schema-generators.complete :as c]
            [solita.etp.test-system :as ts]
            [solita.etp.service.yritys :as service]
            [solita.etp.schema.yritys :as schema]
            [solita.etp.schema.common :as common-schema]))

(t/use-fixtures :each ts/fixture)

(def counter (atom 0))
(defn next-ytunnus []
  (let [value (format "%07d" (swap! counter inc))
        checksum (common-schema/ytunnus-checksum value)]
    (if (= checksum 10) (next-ytunnus) (str value "-" checksum))))

(t/deftest add-and-find-yritys-test
  (doseq [yritys (repeatedly 100 #(c/complete {:ytunnus (next-ytunnus)} schema/YritysSave))
          :let [id (service/add-yritys! ts/*db* yritys)]]
    (t/is (= (assoc yritys :id id) (service/find-yritys ts/*db* id)))))

