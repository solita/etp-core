(ns solita.etp.service.yritys-test
  (:require [clojure.test :as t]
            [schema-generators.complete :as c]
            [solita.etp.test-system :as ts]
            [solita.etp.service.yritys :as service]
            [solita.etp.schema.yritys :as schema]
            [solita.etp.schema.common :as common-schema]))

(t/use-fixtures :each ts/fixture)

(defn unique-ytunnus-range [to]
  (->> (range 0 to)
       (map (partial format "%07d"))
       (filter #(not= (common-schema/ytunnus-checksum %) 10))
       (map #(str % "-" (common-schema/ytunnus-checksum %)))))

(t/deftest add-and-find-yritys-test
  (doseq [yritys (map #(c/complete {:ytunnus %} schema/YritysSave) (unique-ytunnus-range 100))
          :let [id (service/add-yritys! ts/*db* yritys)]]
    (t/is (= (assoc yritys :id id) (service/find-yritys ts/*db* id)))))


(t/deftest add-duplicate-ytunnus
  (let [ytunnus "0000001-9"
        yritys1 (c/complete {:ytunnus ytunnus} schema/YritysSave)
        yritys2 (c/complete {:ytunnus ytunnus} schema/YritysSave)]
    (service/add-yritys! ts/*db* yritys1)
    (t/is (= (ex-data (t/is (thrown? Exception (service/add-yritys! ts/*db* yritys2))))
             {:type       :unique-violation
              :constraint :yritys-ytunnus-key}))))