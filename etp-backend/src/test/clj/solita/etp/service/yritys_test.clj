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

(t/deftest add-and-find-laskutusosoite-test
  (let [yritysid (service/add-yritys! ts/*db* (g/generate schema/YritysSave))]
    (doseq [laskutusosoite (repeatedly 100 #(g/generate schema/LaskutusosoiteSave))
            :let [id (service/add-laskutusosoite! ts/*db* yritysid laskutusosoite)]]
      (t/is (= (assoc laskutusosoite :id id :yritysid yritysid)
               (first (filter #(= id (:id %))
                       (service/find-laskutusosoitteet ts/*db* yritysid))))))))
