(ns solita.etp.service.energiatodistus-test
  (:require [clojure.test :as t]
            [clojure.java.io :as io]
            [solita.etp.test :as etp-test]
            [solita.common.map :as xmap]
            [solita.etp.test-system :as ts]
            [solita.etp.test-data.kayttaja :as kayttaja-test-data]
            [solita.etp.test-data.laatija :as laatija-test-data]
            [solita.etp.test-data.energiatodistus :as energiatodistus-test-data]
            [solita.etp.service.energiatodistus :as service])
  (:import (java.time Instant)
           (clojure.lang ExceptionInfo)))

(t/use-fixtures :each ts/fixture)

(defn test-data-set []
  (let [laatijat (laatija-test-data/generate-and-insert! 1)
        laatija-id (-> laatijat keys sort first)
        energiatodistukset (merge (energiatodistus-test-data/generate-and-insert!
                                   1
                                   2013
                                   true
                                   laatija-id)
                                  (energiatodistus-test-data/generate-and-insert!
                                   1
                                   2018
                                   true
                                   laatija-id))]
    {:laatijat laatijat
     :energiatodistukset energiatodistukset}))

(defn add-eq-found? [add found]
  (xmap/submap? (dissoc add :kommentti)
                (-> found
                    (dissoc :kommentti)
                    (xmap/dissoc-in [:tulokset :e-luokka])
                    (xmap/dissoc-in [:tulokset :e-luku]))))

(defn energiatodistus-tila [id]
  (-> (service/find-energiatodistus ts/*db* id) :tila-id service/tila-key))

(t/deftest add-and-find-energiatodistus-test
  (let [{:keys [laatijat energiatodistukset]} (test-data-set)]
    (doseq [id (-> energiatodistukset keys sort)]
      (t/is (add-eq-found? (get energiatodistukset id)
                           (service/find-energiatodistus ts/*db* id))))))

(t/deftest no-permissions-to-draft-test
  (let [{:keys [energiatodistukset]} (test-data-set)
        id (-> energiatodistukset keys first)]
    (doseq [rooli [kayttaja-test-data/laatija
                   kayttaja-test-data/patevyyden-toteaja
                   kayttaja-test-data/paakayttaja
                   kayttaja-test-data/laskuttaja]]
      (t/is (= (etp-test/catch-ex-data
                #(service/find-energiatodistus ts/*db* rooli id))
               {:type :forbidden})))))

(t/deftest validation-test
  (let [{:keys [laatijat]} (test-data-set)
        laatija-id (-> laatijat keys sort first)
        energiatodistus (-> (energiatodistus-test-data/generate-adds 1
                                                                     2018
                                                                     false)
                            first
                            (assoc-in [:lahtotiedot :ikkunat :etela :U] 99))]
    (t/is (= (etp-test/catch-ex-data
              #(service/add-energiatodistus! (ts/db-user laatija-id)
                                             {:id laatija-id}
                                             2018
                                             energiatodistus))
             {:type :invalid-value
              :message "Property: lahtotiedot.ikkunat.etela.U has an invalid value: 99"}))))

(t/deftest update-energiatodistus-test
  (let [{:keys [laatijat energiatodistukset]} (test-data-set)
        laatija-id (-> laatijat keys sort first)
        id (-> energiatodistukset keys sort first)
        update (first (energiatodistus-test-data/generate-updates 1 2013 false))]
    (service/update-energiatodistus! ts/*db* {:id laatija-id :rooli 0} id update)
    (t/is (add-eq-found? update
                         (service/find-energiatodistus ts/*db* id)))))

(t/deftest delete-test
  (let [{:keys [laatijat energiatodistukset]} (test-data-set)
        laatija-id (-> laatijat keys sort first)
        id (-> energiatodistukset keys sort first)]
    (service/delete-energiatodistus-luonnos! ts/*db*
                                             {:id laatija-id}
                                             id)
    (t/is (nil? (service/find-energiatodistus ts/*db* id)))))

(t/deftest laskuttaja-permissions-test
  (let [{:keys [energiatodistukset]} (test-data-set)
        update (first (energiatodistus-test-data/generate-updates 1 2013 false))
        id (-> energiatodistukset keys sort first)]
    (t/is (= (etp-test/catch-ex-data
              #(service/update-energiatodistus! ts/*db*
                                                kayttaja-test-data/laskuttaja
                                                id
                                                update))
             {:type :forbidden
              :reason (str "Role: :laskuttaja is not allowed to update energiatodistus "
                           id
                           " in state: :draft laskutettu: false")}))))

(t/deftest start-energiatodistus-signing!-test
  (let [{:keys [laatijat energiatodistukset]} (test-data-set)
        laatija-id (-> laatijat keys sort first)
        whoami {:id laatija-id :rooli 0}
        id (-> energiatodistukset keys sort first)
        db (ts/db-user laatija-id)]
    (t/is (= (energiatodistus-tila id) :draft))
    (t/is (= (service/start-energiatodistus-signing! db whoami id) :ok))
    (t/is (= (energiatodistus-tila id) :in-signing))
    (t/is (= (service/start-energiatodistus-signing! db whoami id)
             :already-in-signing))))

(t/deftest stop-energiatodistus-signing!-test
  (let [{:keys [laatijat energiatodistukset]} (test-data-set)
        laatija-id (-> laatijat keys sort first)
        whoami {:id laatija-id :rooli 0}
        id (-> energiatodistukset keys sort first)
        db (ts/db-user laatija-id)]
    (t/is (= (energiatodistus-tila id) :draft))
    (t/is (=  (service/end-energiatodistus-signing! db
                                                    ts/*aws-s3-client*
                                                    whoami
                                                    id)
              :not-in-signing))
    (t/is (= (energiatodistus-tila id) :draft))
    (service/start-energiatodistus-signing! db whoami id)
    (t/is (= (energiatodistus-tila id) :in-signing))
    (energiatodistus-test-data/sign-pdf! id laatija-id)
    (t/is (= (service/end-energiatodistus-signing! db
                                                   ts/*aws-s3-client*
                                                   whoami
                                                   id)
             :ok))
    (t/is (= (energiatodistus-tila id) :signed))
    (t/is (= (service/end-energiatodistus-signing! db
                                                   ts/*aws-s3-client*
                                                   whoami
                                                   id)
             :already-signed))))

(t/deftest cancel-energiatodistus-signing!-test
  (let [{:keys [laatijat energiatodistukset]} (test-data-set)
        laatija-id (-> laatijat keys sort first)
        whoami {:id laatija-id :rooli 0}
        id (-> energiatodistukset keys sort first)
        db (ts/db-user laatija-id)]
    (t/is (= (energiatodistus-tila id) :draft))
    (t/is (=  (service/cancel-energiatodistus-signing! db whoami id)
              :not-in-signing))
    (t/is (= (energiatodistus-tila id) :draft))
    (service/start-energiatodistus-signing! db whoami id)
    (t/is (= (energiatodistus-tila id) :in-signing))
    (t/is (=  (service/cancel-energiatodistus-signing! db whoami id)
              :ok))
    (t/is (= (energiatodistus-tila id) :draft))))

(t/deftest update-signed-energiatodistus!-test
  (let [{:keys [laatijat energiatodistukset]} (test-data-set)
        laatija-id (-> laatijat keys sort first)
        whoami {:id laatija-id :rooli 0}
        db (ts/db-user laatija-id)
        id (-> energiatodistukset keys sort first)
        add (get energiatodistukset id)
        update (-> (energiatodistus-test-data/generate-updates 1 2013 true)
                   first
                   (assoc-in [:perustiedot :rakennustunnus] "103515074X"))]
    (t/is (= (energiatodistus-tila id) :draft))
    (energiatodistus-test-data/sign! id laatija-id true)
    (t/is (= (energiatodistus-tila id) :signed))
    (service/update-energiatodistus! db whoami id update)
    (let [{:keys [allekirjoitusaika voimassaolo-paattymisaika]
           :as energiatodistus} (service/find-energiatodistus ts/*db* id)]
      (t/is (add-eq-found?
             (-> add
                 (assoc-in [:perustiedot :rakennustunnus]
                           (-> update :perustiedot :rakennustunnus))
                 (assoc :laskuriviviite
                        (:laskuriviviite update)
                        :tila-id 2
                        :allekirjoitusaika
                        allekirjoitusaika
                        :voimassaolo-paattymisaika
                        voimassaolo-paattymisaika))
             energiatodistus)))))

(t/deftest korvaa-energiatodistus!-test
  (let [{:keys [laatijat energiatodistukset]} (test-data-set)
        laatija-id (-> laatijat keys sort first)
        whoami {:id laatija-id :rooli 0}
        db (ts/db-user laatija-id)
        korvattava-id (-> energiatodistukset keys sort second)
        korvaava-add (-> (energiatodistus-test-data/generate-adds 1 2018 true)
                         first
                         (assoc :korvattu-energiatodistus-id korvattava-id))
        _ (energiatodistus-test-data/sign! korvattava-id laatija-id true)
        korvaava-id (first (energiatodistus-test-data/insert! [korvaava-add]
                                                              laatija-id))]
    (energiatodistus-test-data/sign! korvaava-id laatija-id true)
    (t/is (= (energiatodistus-tila korvaava-id) :signed))
    (t/is (= (energiatodistus-tila korvattava-id) :replaced))))

(t/deftest korvaa-energiatodistus-states-test
  (let [{:keys [laatijat]} (test-data-set)
        laatija-id (-> laatijat keys sort first)
        whoami {:id laatija-id :rooli 0}
        db (ts/db-user laatija-id)
        energiatodistus (first (energiatodistus-test-data/generate-adds 1
                                                                        2018
                                                                        true))
        [korvattu-id korvattava-id
         luonnos-id update-id] (energiatodistus-test-data/insert!
                                (repeat 4 energiatodistus)
                                laatija-id)
        _ (energiatodistus-test-data/sign! korvattu-id laatija-id true)
        korvannut-id (energiatodistus-test-data/insert!
                      [(assoc energiatodistus
                              :korvattu-energiatodistus-id
                              korvattu-id)]
                      laatija-id)
        _ (energiatodistus-test-data/sign! korvannut-id laatija-id true)
        _ (energiatodistus-test-data/sign! korvattava-id laatija-id true)]

    ;; Create energiatodistus with illegals and valid replaceable energiatodistus
    (t/is (= (etp-test/catch-ex-data
              #(energiatodistus-test-data/insert!
                [(assoc energiatodistus
                        :korvattu-energiatodistus-id
                        101)]
                laatija-id))
             {:type :invalid-replace
              :message "Replaceable energiatodistus 101 does not exist"}))
    (t/is (= (etp-test/catch-ex-data
              #(energiatodistus-test-data/insert!
                [(assoc energiatodistus
                        :korvattu-energiatodistus-id
                        luonnos-id)]
                laatija-id))
             {:type :invalid-replace
              :message (str "Replaceable energiatodistus "
                            luonnos-id
                            " is not in signed or discarded state")}))
    (t/is (= (etp-test/catch-ex-data
              #(energiatodistus-test-data/insert!
                [(assoc energiatodistus
                        :korvattu-energiatodistus-id
                        korvattu-id)]
                laatija-id))
             {:type :invalid-replace
              :message (str "Replaceable energiatodistus "
                            korvattu-id
                            " is already replaced")}))

    ;; Update energiatodistus with illegals and valid replaceable
    ;; energiatodistus
    (t/is (= (etp-test/catch-ex-data
              #(service/update-energiatodistus!
                ts/*db*
                whoami
                update-id
                (assoc energiatodistus
                       :korvattu-energiatodistus-id
                       101)))
             {:type :invalid-replace
              :message "Replaceable energiatodistus 101 does not exist"}))
    (t/is (= (etp-test/catch-ex-data
              #(service/update-energiatodistus!
                ts/*db*
                whoami
                update-id
                (assoc energiatodistus
                       :korvattu-energiatodistus-id
                       luonnos-id)))
             {:type :invalid-replace
              :message (str "Replaceable energiatodistus "
                            luonnos-id
                            " is not in signed or discarded state")}))
    (t/is (= (etp-test/catch-ex-data
              #(service/update-energiatodistus!
                ts/*db*
                whoami
                update-id
                (assoc energiatodistus
                       :korvattu-energiatodistus-id
                       korvattu-id)))
             {:type :invalid-replace
              :message (str "Replaceable energiatodistus "
                            korvattu-id
                            " is already replaced")}))

    ;; Check states of energiatodistukset
    (t/is (= (energiatodistus-tila korvattava-id) :signed))
    (t/is (= (energiatodistus-tila korvattu-id) :replaced))
    (t/is (= (energiatodistus-tila korvannut-id) :signed))
    (t/is (= (energiatodistus-tila luonnos-id) :draft))
    (t/is (= (energiatodistus-tila update-id) :draft))))

(t/deftest set-energiatodistus-discarded!-test
  (let [{:keys [laatijat energiatodistukset]} (test-data-set)
        laatija-id (-> laatijat keys sort first)
        whoami {:id laatija-id :rooli 0}
        db (ts/db-user laatija-id)
        id (-> energiatodistukset keys sort first)
        add (get energiatodistukset id)]
    (t/is (= (energiatodistus-tila id) :draft))
    (energiatodistus-test-data/sign! id laatija-id true)
    (t/is (= (energiatodistus-tila id) :signed))
    (service/set-energiatodistus-discarded! db id true)
    (t/is (= (energiatodistus-tila id) :discarded))
    (t/is (add-eq-found? (get energiatodistukset id)
                         (service/find-energiatodistus ts/*db* id)))
    (service/set-energiatodistus-discarded! db id false)
    (t/is (= (energiatodistus-tila id) :signed))))

(t/deftest validate-pdf-signature
  (t/is (= true (#'service/pdf-signed? (-> "energiatodistukset/signed-with-test-certificate.pdf"
                                           io/resource
                                           io/input-stream))))
  (t/is (= false (#'service/pdf-signed? (-> "energiatodistukset/not-signed.pdf"
                                            io/resource
                                            io/input-stream)))))
