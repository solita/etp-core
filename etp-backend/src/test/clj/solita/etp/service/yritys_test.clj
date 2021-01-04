(ns solita.etp.service.yritys-test
  (:require [clojure.test :as t]
            [schema-generators.complete :as c]
            [schema.core :as schema]
            [solita.etp.test-system :as ts]
            [solita.etp.test :as test]
            [solita.etp.service.yritys :as service]
            [solita.etp.service.kayttaja :as kayttaja-service]
            [solita.etp.schema.yritys :as yritys-schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.service.energiatodistus-test :as energiatodistus-test]))

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
  (let [laatija-id (energiatodistus-test/add-laatija!)]
  (doseq [yritys (generate-yritykset 100)
          :let [id (service/add-yritys! (ts/db-user laatija-id)
                                        {:id laatija-id} yritys)]]
    (t/is (= (assoc yritys :id id) (service/find-yritys ts/*db* id))))))

(t/deftest add-duplicate-ytunnus-nimi-vastaanottaja-tarkenne
  (let [laatija-id (energiatodistus-test/add-laatija!)
        [yritys-1 yritys-2] (->> (generate-yritykset 2)
                                 (map #(assoc % :ytunnus "0000001-9"))
                                 (map #(assoc % :nimi "test"))
                                 (map #(assoc % :vastaanottajan-tarkenne "test")))]
    (service/add-yritys! ts/*db* {:id laatija-id} yritys-1)
    (t/is (= (test/catch-ex-data #(service/add-yritys! ts/*db* {:id laatija-id} yritys-2))
             {:type       :unique-violation
              :constraint :yritys-ytunnus-nimi-vastaanottajan-tarkenne-key,
              :value "0000001-9, test, test"}))))

(defn dissoc-not-modified [yritys] (dissoc yritys :ytunnus))

(t/deftest update-yritys-test
  (let [laatija-id (energiatodistus-test/add-laatija!)
        paakayttaja-id (kayttaja-service/add-kayttaja!
                         ts/*db* {:rooli 2 :etunimi "P" :sukunimi "P"
                                  :puhelin "1" :email "f.com"})
        [yritys-1 yritys-2 yritys-3] (generate-yritykset 3)
        id (service/add-yritys! (ts/db-user laatija-id)  {:id laatija-id} yritys-1)]
    (t/is (= (assoc yritys-1 :id id) (service/find-yritys ts/*db* id)))

    (service/update-yritys! (ts/db-user laatija-id) {:id laatija-id} id yritys-2)
    (t/is (= (-> yritys-2 (assoc :id id) dissoc-not-modified)
             (dissoc-not-modified (service/find-yritys ts/*db* id))))

    (service/update-yritys! (ts/db-user paakayttaja-id) {:rooli 2} id yritys-3)
    (t/is (= (-> yritys-3 (assoc :id id) dissoc-not-modified)
             (dissoc-not-modified (service/find-yritys ts/*db* id))))

    (t/is (= (-> (test/catch-ex-data #(service/update-yritys!
                                    ts/*db* {:rooli 0}
                                    id yritys-2))
                 (dissoc :reason))
             {:type :forbidden}))))

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
