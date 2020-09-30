(ns solita.etp.service.yritys-test
  (:require [clojure.test :as t]
            [schema-generators.complete :as c]
            [schema.core :as schema]
            [solita.etp.test-system :as ts]
            [solita.etp.service.yritys :as service]
            [solita.etp.schema.yritys :as yritys-schema]
            [solita.etp.schema.common :as common-schema]))

(t/use-fixtures :each ts/fixture)

(defn unique-ytunnus-range [to]
  (->> (range 0 to)
       (map (partial format "%07d"))
       (filter #(not= (common-schema/ytunnus-checksum %) 10))
       (map #(str % "-" (common-schema/ytunnus-checksum %)))))

(defn generate-yritykset [n]
  (map #(c/complete {:ytunnus %
                     :verkkolaskuoperaattori (rand-int 32)
                     :verkkolaskuosoite "003712345671"
                     :maa "FI"} yritys-schema/YritysSave)
       (unique-ytunnus-range n)))

(t/deftest add-and-find-yritys-test
  (doseq [yritys (generate-yritykset 100)
          :let [id (service/add-yritys! ts/*db* yritys)]]
    (t/is (= (assoc yritys :id id) (service/find-yritys ts/*db* id)))))

#_(t/deftest add-duplicate-ytunnus
  (let [ytunnus "0000001-9"
        [yritys-1 yritys-2] (->> (generate-yritykset 2)
                                 (map #(assoc % :ytunnus ytunnus)))]
    (service/add-yritys! ts/*db* yritys-1)
    (t/is (= (ex-data (t/is (thrown? Exception (service/add-yritys! ts/*db* yritys-2))))
             {:type       :unique-violation
              :constraint :yritys-ytunnus-key}))))

(t/deftest find-all-laskutuskielet-test
  (let [laskutuskielet (service/find-all-laskutuskielet ts/*db*)]
    (t/is (= 3 (count laskutuskielet)))
    (t/is (schema/validate [common-schema/Luokittelu]
                           laskutuskielet))))

(t/deftest find-all-verkkolaskuoperaattorit-test
  (let [verkkolaskuoperaattorit (service/find-all-verkkolaskuoperaattorit ts/*db*)]
    (t/is (= 32 (count verkkolaskuoperaattorit)))
    (t/is (schema/validate [yritys-schema/Verkkolaskuoperaattori]
                           verkkolaskuoperaattorit))))
