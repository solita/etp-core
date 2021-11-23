(ns solita.etp.service.valvonta-oikeellisuus-test
  (:require [clojure.test :as t]
            [schema.core :as schema]
            [solita.etp.test-system :as ts]
            [solita.etp.test-data.kayttaja :as kayttaja-test-data]
            [solita.etp.test-data.laatija :as laatija-test-data]
            [solita.etp.test-data.energiatodistus :as energiatodistus-test-data]
            [solita.etp.schema.valvonta-oikeellisuus :as vo-schema]
            [solita.etp.service.valvonta-oikeellisuus :as service]))

(t/use-fixtures :each ts/fixture)

(defn test-data-set []
  (let [paakayttajat [(-> (kayttaja-test-data/generate-adds 1)
                          first
                          (merge kayttaja-test-data/paakayttaja))]
        paakayttaja-ids (kayttaja-test-data/insert! paakayttajat)
        laatijat (laatija-test-data/generate-and-insert! 2)
        laatija-ids (-> laatijat keys sort)
        [laatija-id-1 laatija-id-2] laatija-ids
        et-adds (energiatodistus-test-data/generate-adds 5 2018 true)
        et-ids (->> (interleave (cycle laatija-ids) et-adds)
                    (partition 2)
                    (mapcat (fn [[laatija-id et-add]]
                              (let [et-id (energiatodistus-test-data/insert! [et-add] laatija-id)]
                                (energiatodistus-test-data/sign! et-id laatija-id true)
                                et-id))))]
    {:laatijat laatijat
     :paakayttajat (zipmap paakayttaja-ids paakayttajat)
     :energiatodistukset (zipmap et-ids et-adds)}))

(defn paakayttaja-whoami [paakayttaja-id]
  {:rooli 2 :id paakayttaja-id})

(defn virheet->valvontamuistio [virheet]
  {:type-id 7
   :deadline-date nil
   :template-id 1 ; TODO
   :description "ETP tests"
   :virheet virheet
   :severity-id 1
   :tiedoksi []})

(defn add-toimenpide! [whoami et-id & virheet]
  (let [db (ts/db-user (:id whoami))
        valvontamuistio-add (schema.core/validate vo-schema/ToimenpideAdd
                                                  (virheet->valvontamuistio virheet))
        {:keys [id]}
        (service/add-toimenpide! db
                                 ts/*aws-s3-client*
                                 whoami et-id valvontamuistio-add)]
    (service/publish-toimenpide! db ts/*aws-s3-client* whoami et-id id)
    id))

(defn tilasto-item-has-virhetype-id [virhetype-id]
  (fn [tilasto-item]
    (= virhetype-id (:type-id tilasto-item))))

(t/deftest virhetilastot-empty-test
  (let [{:keys [laatijat energiatodistukset]} (test-data-set)]
    (t/is (empty? (service/virhetilastot ts/*db*)))))

(t/deftest virhetilastot-test
  (let [{:keys [laatijat energiatodistukset paakayttajat]} (test-data-set)
        [et-1-id et-2-id et-3-id] (keys energiatodistukset)
        [paakayttaja-id _] (first paakayttajat)
        whoami (paakayttaja-whoami paakayttaja-id)
        [virhe-1-id virhe-2-id virhe-3-id] (->> (service/find-virhetypes ts/*db*)
                                                (map :id)
                                                shuffle
                                                (take 3))]
    (t/is (empty? (service/virhetilastot ts/*db*)))
    (t/is (int? et-3-id))
    (t/is (int? (add-toimenpide! whoami et-1-id
                                 {:description "ET 1 Virhe 1"
                                  :type-id virhe-1-id})))
    (t/is (int? (add-toimenpide! whoami et-2-id
                                 {:description "ET 2 Virhe 1"
                                  :type-id virhe-1-id})))
    (t/is (int? (add-toimenpide! whoami et-3-id
                                 {:description "ET 3 Virhe 2"
                                  :type-id virhe-2-id})))
    (let [tilastot (service/virhetilastot ts/*db*)]
      (t/is (= 2 (->> tilastot
                      (filter (tilasto-item-has-virhetype-id virhe-1-id))
                      first
                      :count)))
      (t/is (= 1 (->> tilastot
                      (filter (tilasto-item-has-virhetype-id virhe-2-id))
                      first
                      :count)))
      (t/is (nil? (->> tilastot
                       (filter (tilasto-item-has-virhetype-id virhe-3-id))
                       first
                       :count))))))
