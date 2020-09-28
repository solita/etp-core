(ns solita.etp.service.complete-energiatodistus-test
  (:require [clojure.test :as t]
            [solita.etp.test-system :as ts]
            [solita.etp.service.energiatodistus :as eservice]
            [solita.etp.service.complete-energiatodistus :as service]
            [solita.etp.service.energiatodistus-test :as energiatodistus-test]))

(t/use-fixtures :each ts/fixture)

(def generate-energiatodistus
  #(->
    (energiatodistus-test/generate-energiatodistus-2018-complete)
    (assoc-in [:tulokset
               :kaytettavat-energiamuodot
               :kaukolampo]
              1500)
    (assoc-in [:lahtotiedot :ilmanvaihto :paaiv :tulo] 15)
    (assoc-in [:lahtotiedot :ilmanvaihto :paaiv :poisto] 35)))

(defn assert-complete-energiatoditus [complete-energiatodistus]
  (and (= 750.0 (-> complete-energiatodistus
                    :tulokset
                    :kaytettavat-energiamuodot
                    :kaukolampo-kertoimella))
       (= "15 / 35" (-> complete-energiatodistus
                        :lahtotiedot
                        :ilmanvaihto
                        :paaiv
                        :tulo-poisto))))

(t/deftest complete-energiatodistus-test
  (let [laatija-id (energiatodistus-test/add-laatija!)
        {:keys [kielisyydet
                laatimisvaiheet
                alakayttotarkoitukset]} (service/required-luokittelut ts/*db*)
        complete-energiatodistus (service/complete-energiatodistus
                                  ts/*db*
                                  (generate-energiatodistus)
                                  kielisyydet
                                  laatimisvaiheet
                                  alakayttotarkoitukset)]
    (t/is (assert-complete-energiatoditus complete-energiatodistus))))


(t/deftest find-complete-energiatodistus-test
  (let [laatija-id (energiatodistus-test/add-laatija!)
        id (energiatodistus-test/add-energiatodistus! (generate-energiatodistus)
                                                      laatija-id)
        whoami {:id laatija-id :rooli 0}
        complete-energiatodistus (service/find-complete-energiatodistus ts/*db*
                                                                        whoami
                                                                        id)]
    (t/is (assert-complete-energiatoditus complete-energiatodistus))))

(t/deftest find-complete-energiatodistukset-by-laatija-test
  (let [laatija-id (energiatodistus-test/add-laatija!)
        _ (energiatodistus-test/add-energiatodistus! (generate-energiatodistus) laatija-id)
        _ (energiatodistus-test/add-energiatodistus! (generate-energiatodistus) laatija-id)
        complete-energiatodistukset (service/find-complete-energiatodistukset-by-laatija
                                     ts/*db*
                                     laatija-id
                                     nil)]
    (t/is (= 2 (count complete-energiatodistukset)))
    (t/is (every? assert-complete-energiatoditus complete-energiatodistukset))))
