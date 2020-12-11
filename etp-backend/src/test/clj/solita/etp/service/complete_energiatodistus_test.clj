(ns solita.etp.service.complete-energiatodistus-test
  (:require [clojure.test :as t]
            [solita.etp.test-system :as ts]
            [solita.etp.service.energiatodistus :as eservice]
            [solita.etp.service.complete-energiatodistus :as service]
            [solita.etp.service.energiatodistus-test :as energiatodistus-test]
            [solita.common.map :as xmap]))

(t/use-fixtures :each ts/fixture)

(def generate-energiatodistus
  #(->
    (energiatodistus-test/generate-energiatodistus-2018-complete)
    (assoc-in [:tulokset
               :kaytettavat-energiamuodot
               :kaukolampo]
              1500)
    (assoc-in [:lahtotiedot :ilmanvaihto :paaiv :tulo] 15)
    (assoc-in [:lahtotiedot :ilmanvaihto :paaiv :poisto] 35)
    (xmap/dissoc-in [:lahtotiedot :ilmanvaihto :erillispoistot :tulo])
    (assoc-in [:lahtotiedot :ilmanvaihto :erillispoistot :poisto] 12.34)))

(defn assert-complete-energiatoditus [complete-energiatodistus]
  (and (= 750.0M (-> complete-energiatodistus
                    :tulokset
                    :kaytettavat-energiamuodot
                    :kaukolampo-kertoimella))
       (= "15.000 / 35.000" (-> complete-energiatodistus
                                :lahtotiedot
                                :ilmanvaihto
                                :paaiv
                                :tulo-poisto))
       (= " / 12.340" (-> complete-energiatodistus
                                :lahtotiedot
                                :ilmanvaihto
                                :erillispoistot
                                :tulo-poisto))))

(t/deftest complete-energiatodistus-test
  (let [luokittelut (service/required-luokittelut ts/*db*)
        complete-energiatodistus (service/complete-energiatodistus
                                  (generate-energiatodistus)
                                  luokittelut)]
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
