(ns solita.etp.service.sivu-test
  (:require [clojure.test :as t]
            [solita.common.map :as xmap]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.service.sivu :as service]
            [solita.etp.test-data.kayttaja :as kayttaja-test-data]
            [solita.etp.test-data.laatija :as laatija-test-data]
            [solita.etp.test-data.sivu :as sivu-test-data]
            [solita.etp.test-system :as ts]))

(t/use-fixtures :each ts/fixture)

(defn test-data-set []
  (let [paakayttaja-adds (->> (kayttaja-test-data/generate-adds 2)
                              (map #(assoc % :rooli 2)))
        paakayttaja-ids (kayttaja-test-data/insert! paakayttaja-adds)
        laskuttaja-adds (->> (kayttaja-test-data/generate-adds 2)
                             (map #(assoc % :rooli 3)))
        laskuttaja-ids (kayttaja-test-data/insert! laskuttaja-adds)
        laatijat (laatija-test-data/generate-and-insert! 10)
        published-sivut (sivu-test-data/generate-and-insert!
                         2 3 nil
                         #(assoc % :published true))
        unpublished-sivut (sivu-test-data/generate-and-insert!
                           1 3 nil
                           #(assoc % :published false))
        sivut (concat published-sivut unpublished-sivut)]
    {:paakayttajat (zipmap paakayttaja-ids paakayttaja-adds)
     :laskuttajat (zipmap laskuttaja-ids laskuttaja-adds)
     :laatijat laatijat
     :sivut sivut
     :published-sivut published-sivut
     :unpublished-sivut unpublished-sivut}))

(t/deftest find-sivu
  (let [ds (test-data-set)
        sivut-by-id (->> (:sivut ds)
                         (group-by :id)
                         (xmap/map-values first))
        laatija {:rooli 0}
        paakayttaja {:rooli 2}
        unpub-id (-> ds :unpublished-sivut first :id)]
    (t/is (nil? (service/find-sivu ts/*db*
                                   laatija
                                   unpub-id)))

    (t/is (= (get sivut-by-id unpub-id)
             (service/find-sivu ts/*db*
                                paakayttaja
                                unpub-id)))))

(t/deftest find-all
  (let [ds (test-data-set)
        laatija {:rooli 0}
        paakayttaja {:rooli 2}]
    ;; laatija sees only published sivut
    (t/is (= (->> (service/find-all-sivut ts/*db* laatija)
                 (map :id) set)
             (->> (:published-sivut ds)
                  (map :id) set)))

    ;; paakayttaja sees every page
    (t/is (= (->> (service/find-all-sivut ts/*db* paakayttaja)
                 (map :id) set)
             (->> (:sivut ds)
                  (map :id) set)))))

(t/deftest update-body
  (let [ds (test-data-set)
        sivu (-> ds :sivut first)
        sivu-id (:id sivu)
        new-body "Lorem ipsum"
        paakayttaja {:rooli 2}]
    (t/is (= sivu (service/find-sivu ts/*db* paakayttaja sivu-id)))
    (service/update-sivu! ts/*db* sivu-id {:body new-body})
    (t/is (= (assoc sivu :body new-body)
             (service/find-sivu ts/*db* paakayttaja sivu-id)))))

(t/deftest update-title
  (let [ds (test-data-set)
        sivu (-> ds :sivut first)
        sivu-id (:id sivu)
        new-title "Hello"
        paakayttaja {:rooli 2}]
    (t/is (= sivu (service/find-sivu ts/*db* paakayttaja sivu-id)))
    (service/update-sivu! ts/*db* sivu-id {:title new-title})
    (t/is (= (assoc sivu :title new-title)
             (service/find-sivu ts/*db* paakayttaja sivu-id)))))

(t/deftest find-empty
  (t/is (empty? (service/find-all-sivut ts/*db* {:rooli 2 :id 1}))))

(t/deftest find-one-after-add
  (let [whoami {:rooli 2 :id 1}
        sivu-in {:title "Pääsääntö"
                 :body "Äläpä tee virheitä"
                 :published true
                 :ordinal 4
                 :parent-id nil}
        added-sivu-id (:id (service/add-sivu! ts/*db* sivu-in))
        sivu-out (service/find-sivu ts/*db* whoami added-sivu-id)]
    (t/is (not (nil? added-sivu-id)))
    (t/is (not (nil? sivu-out)))
    (t/is (= (:id    sivu-out) added-sivu-id))
    (t/is (= (:title sivu-out) (:title sivu-in)))
    (t/is (= (:body  sivu-out) (:body  sivu-in)))
    (t/is (:published sivu-out))))

(t/deftest simple-hierarchy
  (let [whoami {:rooli 2 :id 1}
        defaults {:published false
                  :ordinal 1
                  :parent-id nil}
        root-1-in (assoc defaults
                         :title "Lämmitysjärjestelmät"
                         :body "Miten saadaan lämpöenergiaa sisään")
        root-1-id (:id (service/add-sivu! ts/*db* root-1-in))
        child-1-1-in (assoc defaults
                            :title "Takat"
                            :body "Poltetaan asioita"
                            :parent-id root-1-id
                            :ordinal 1)
        child-1-1-id (service/add-sivu! ts/*db* child-1-1-in)
        child-1-2-in (assoc defaults
                            :title "Koirat"
                            :body "Nämäkin luovuttavat lämpöenergiaa"
                            :parent-id root-1-id
                            :ordinal 2)
        child-1-2-id (service/add-sivu! ts/*db* child-1-2-in)
        root-2-in (assoc defaults
                         :title "Eristäminen"
                         :body "Miten saadaan energia pysymään sisällä?")
        root-2-id (:id (service/add-sivu! ts/*db* root-2-in))
        sivu-list (service/find-all-sivut ts/*db* whoami)]
    (t/is (not (empty? sivu-list)))
    (t/is (= (:body (service/find-sivu ts/*db* whoami (:id child-1-2-id)))
             (:body child-1-2-in)))))
