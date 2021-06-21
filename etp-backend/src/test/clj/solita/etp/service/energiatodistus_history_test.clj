(ns solita.etp.service.energiatodistus-history-test
  (:require [clojure.test :as t]
            [solita.etp.test-system :as ts]
            [solita.etp.test-data.laatija :as laatija-test-data]
            [solita.etp.test-data.energiatodistus :as energiatodistus-test-data]
            [solita.etp.service.energiatodistus :as energiatodistus-service]
            [solita.etp.service.energiatodistus-history :as service])
  (:import (java.time Instant)))

(t/use-fixtures :each ts/fixture)

(defn update-energiatodistus! [energiatodistus-id energiatodistus laatija-id]
  (energiatodistus-service/update-energiatodistus! (ts/db-user laatija-id)
                                                   {:id laatija-id :rooli 0}
                                                   energiatodistus-id
                                                   energiatodistus))

(defn test-data-set []
  (let [laatijat (laatija-test-data/generate-and-insert! 3)
        laatija-ids (-> laatijat keys sort)
        [laatija-id-1 laatija-id-2] laatija-ids
        whoami-1 {:id laatija-id-1 :rooli 0}
        whoami-2 {:id laatija-id-2 :rooli 0}
        energiatodistus-adds (energiatodistus-test-data/generate-adds 3
                                                                      2018
                                                                      true)
        energiatodistus-ids (->> (interleave laatija-ids energiatodistus-adds)
                                 (partition 2)
                                 (mapcat #(energiatodistus-test-data/insert!
                                           [(second %)]
                                           (first %))))
        [energiatodistus-id-1 energiatodistus-id-2] energiatodistus-ids
        [energiatodistus-add-1 energiatodistus-add-2] energiatodistus-adds
        energiatodistus-update-2 (assoc energiatodistus-add-2
                                        :korvattu-energiatodistus-id
                                        energiatodistus-id-1)]

    ;; Update energiatodistus 1 nettoala
    (update-energiatodistus! energiatodistus-id-1
                             (assoc-in energiatodistus-add-1
                                       [:lahtotiedot :lammitetty-nettoala]
                                       123.45)
                             laatija-id-1)

    ;; Sign energiatodistus 1
    (energiatodistus-test-data/sign! energiatodistus-id-1 laatija-id-1 true)

    ;; Update energiatodistus 2 nettoala and replace energiatodistus 1 with 2
    (update-energiatodistus! energiatodistus-id-2
                             (assoc-in energiatodistus-update-2
                                       [:lahtotiedot :lammitetty-nettoala]
                                       678.91)
                             laatija-id-2)

    ;; Update energiatodistus 2 nettoala back to what it was originally
    (update-energiatodistus! energiatodistus-id-2
                             energiatodistus-update-2
                             laatija-id-2)

    ;; Sign energiatodistus 2
    (energiatodistus-test-data/sign! energiatodistus-id-2 laatija-id-2 true)

    ;; Update laskuriviviite of energiatodistus 2
    (update-energiatodistus! energiatodistus-id-2
                             (assoc energiatodistus-update-2
                                    :laskuriviviite
                                    "laskuriviviite")
                             laatija-id-2)
    {:laatijat laatijat
     :energiatodistukset (zipmap energiatodistus-ids energiatodistus-adds)}))

(t/deftest audit-row->flat-energiatodistus-test
  (let [{:keys [energiatodistukset]} (test-data-set)
        id (-> energiatodistukset keys sort first)
        flats (->> (service/find-audit-rows ts/*db* id)
                                     (map service/audit-row->flat-energiatodistus))]
    (doseq [flat-energiatodistus flats]
      (t/is (contains? flat-energiatodistus :id))
      (t/is (contains? flat-energiatodistus :lahtotiedot$lammitetty-nettoala)))))

(t/deftest audit-event-test
  (let [now (Instant/now)]
    (t/is (= {:modifiedby-fullname "Cooper, Dale"
              :modifytime now
              :k :foo
              :v 123}
             (service/audit-event "Cooper, Dale" now :foo 123)))))

(t/deftest find-audit-rows-test
  (let [{:keys [energiatodistukset]} (test-data-set)
        ids (-> energiatodistukset keys sort)
        [id-1 id-2] ids
        audit-rows-1 (service/find-audit-rows ts/*db* id-1)
        audit-rows-2 (service/find-audit-rows ts/*db* id-2)]
    (t/is (= 5 (count audit-rows-1)))
    (t/is (= 6 (count audit-rows-2)))
    (t/is (= [0 0 1 2 4] (map :tila-id audit-rows-1)))
    (t/is (= [0 0 0 1 2 2] (map :tila-id audit-rows-2)))))

(defn without-modifytimes [coll]
  (map #(dissoc % :modifytime) coll))

(defn fullname [laatijat laatija-id]
  (let [{:keys [etunimi sukunimi]} (get laatijat laatija-id)]
    (str sukunimi ", " etunimi)))

(t/deftest find-history-test
  (let [{:keys [laatijat energiatodistukset]} (test-data-set)
        laatija-ids (-> laatijat keys sort)
        [laatija-id-1 laatija-id-2 laatija-id-3] laatija-ids
        [laatija-1-fullname
         laatija-2-fullname
         laatija-3-fullname] (map (partial fullname laatijat) laatija-ids)
        [energiatodistus-id-1
         energiatodistus-id-2
         energiatodistus-id-3] (-> energiatodistukset keys sort)
        history-1 (service/find-history ts/*db* energiatodistus-id-1)
        history-2 (service/find-history ts/*db* energiatodistus-id-2)
        history-3 (service/find-history ts/*db* energiatodistus-id-3)]

    ;; Energiatodistus 1 state history
    (t/is (= 7 (-> history-1 :state-history count)))
    (t/is (= {:modifiedby-fullname laatija-1-fullname :k :tila-id :v 0}
             (-> history-1 :state-history first (dissoc :modifytime))))
    (t/is (= {:modifiedby-fullname laatija-1-fullname :k :tila-id :v 1}
            (-> history-1 :state-history second (dissoc :modifytime))))
    (t/is (= :voimassaolo-paattymisaika
             (-> history-1 :state-history (nth 2) :k)))
    (t/is (= {:modifiedby-fullname laatija-1-fullname :k :tila-id :v 2}
             (-> history-1 :state-history (nth 3) (dissoc :modifytime))))
    (t/is (= :allekirjoitusaika (-> history-1 :state-history (nth 4) :k)))
    (t/is (= {:modifiedby-fullname laatija-2-fullname :k :tila-id :v 4}
             (-> history-1 :state-history (nth 5) (dissoc :modifytime))))
    (t/is (= {:modifiedby-fullname laatija-2-fullname
              :k :korvaava-energiatodistus-id :v energiatodistus-id-2}
             (-> history-1 :state-history last (dissoc :modifytime))))

    ;; Energiatodistus 1 form history
    (t/is (= [{:modifiedby-fullname laatija-1-fullname
               :k :tulokset$e-luku
               :v 1}
              {:modifiedby-fullname laatija-1-fullname
               :k :lahtotiedot$lammitetty-nettoala
               :v 123.45M}]
             (->> history-1 :form-history (map #(dissoc % :modifytime)))))

    ;; Energiatodistus 2 state history
    (t/is (= 5 (-> history-2 :state-history count)))
    (t/is (= {:modifiedby-fullname laatija-2-fullname :k :tila-id :v 0}
             (-> history-2 :state-history first (dissoc :modifytime))))
    (t/is (= {:modifiedby-fullname laatija-2-fullname :k :tila-id :v 1}
             (-> history-2 :state-history second (dissoc :modifytime))))
    (t/is (= :voimassaolo-paattymisaika
             (-> history-2 :state-history (nth 2) :k)))
    (t/is (= {:modifiedby-fullname laatija-2-fullname :k :tila-id :v 2}
             (-> history-2 :state-history (nth 3) (dissoc :modifytime))))
    (t/is (= :allekirjoitusaika (-> history-2 :state-history last :k)))


    ;; Energiatodistus 2 form history
    (t/is (= [{:modifiedby-fullname laatija-2-fullname
               :k :korvattu-energiatodistus-id
               :v energiatodistus-id-1}
              {:modifiedby-fullname laatija-2-fullname
               :k :laskuriviviite
               :v "laskuriviviite"}]
             (->> history-2 :form-history (map #(dissoc % :modifytime)))))))
