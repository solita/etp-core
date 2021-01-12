(ns solita.etp.service.liite-test
  (:require [clojure.test :as t]
            [clojure.java.io :as io]
            [solita.etp.test-system :as ts]
            [solita.etp.service.liite :as liite-service]
            [solita.etp.service.file :as file-service]
            [solita.etp.service.energiatodistus-test :as energiatodistus-test]
            [solita.etp.service.valvonta :as valvonta-service]))

(t/use-fixtures :each ts/fixture)

(def test-file-liitteet [{:size 3000
                          :tempfile (io/file "deps.edn")
                          :contenttype "application/octet-stream"
                          :nimi "deps.edn"}
                         {:size 3000
                          :tempfile (io/file "Dockerfile")
                          :contenttype "application/octet-stream"
                          :nimi "Dockerfile"}])

(def test-link-liite {:nimi "Esimerkki"
                      :url "https://example.com"})

(def paakayttaja {:rooli 2})

(defn add-laatija! []
  {:id (energiatodistus-test/add-laatija!)
   :rooli 0})

(defn add-energiatodistus! [whoami]
  (-> (energiatodistus-test/generate-energiatodistus-2018)
      (energiatodistus-test/add-energiatodistus! (:id whoami) 2018)))

(t/deftest add-liitteet-from-files-and-find-test
  (let [whoami (add-laatija!)
        energiatodistus-id (add-energiatodistus! whoami)
        _ (valvonta-service/update-valvonta! ts/*db* energiatodistus-id true)
        _ (liite-service/add-liitteet-from-files (ts/db-user (:id whoami))
                                                 ts/*aws-s3-client*
                                                 whoami
                                                 energiatodistus-id
                                                 test-file-liitteet)
        liitteet (liite-service/find-energiatodistus-liitteet
                  ts/*db*
                  whoami
                  energiatodistus-id)
        nimet (->> liitteet (map :nimi) (into #{}))]
    (t/is (contains? nimet (-> test-file-liitteet first :nimi)))
    (t/is (contains? nimet (-> test-file-liitteet second :nimi)))))

(t/deftest add-liite-from-link-and-find-test
  (let [whoami (add-laatija!)
        energiatodistus-id (add-energiatodistus! whoami)
        _ (valvonta-service/update-valvonta! ts/*db* energiatodistus-id true)
        _ (liite-service/add-liite-from-link! (ts/db-user (:id whoami))
                                              whoami
                                              energiatodistus-id
                                              test-link-liite)
        liitteet (liite-service/find-energiatodistus-liitteet
                  ts/*db*
                  whoami
                  energiatodistus-id)]
    (t/is (= 1 (count liitteet)))
    (t/is (= test-link-liite (-> liitteet first (select-keys [:nimi :url]))))))

(t/deftest find-energiatodistus-liite-content-test
  (let [whoami (add-laatija!)
        energiatodistus-id (add-energiatodistus! whoami)
        _ (valvonta-service/update-valvonta! ts/*db* energiatodistus-id true)
        _ (liite-service/add-liitteet-from-files (ts/db-user (:id whoami))
                                                 ts/*aws-s3-client*
                                                 whoami
                                                 energiatodistus-id
                                                 [(first test-file-liitteet)])
        liite-id (-> (liite-service/find-energiatodistus-liitteet
                      ts/*db*
                      whoami
                      energiatodistus-id)
                     first
                     :id)
        liite (liite-service/find-energiatodistus-liite-content
               ts/*db*
               whoami
               ts/*aws-s3-client*
               liite-id)]
    (t/is (= (-> liite :content .readAllBytes vec)
             (-> test-file-liitteet
                 first
                 :tempfile
                 file-service/file->byte-array
                 vec)))))

(t/deftest delete-liite-test
  (let [whoami (add-laatija!)
        energiatodistus-id (add-energiatodistus! whoami)
        _ (valvonta-service/update-valvonta! ts/*db* energiatodistus-id true)
        _ (liite-service/add-liite-from-link! (ts/db-user (:id whoami))
                                              whoami
                                              energiatodistus-id
                                              test-link-liite)
        liitteet-before-delete (liite-service/find-energiatodistus-liitteet
                                ts/*db*
                                whoami
                                energiatodistus-id)
        _ (valvonta-service/update-valvonta! ts/*db* energiatodistus-id false)
        _ (t/is (thrown-with-msg? clojure.lang.ExceptionInfo
                                  #"Forbidden"
                                  (liite-service/delete-liite (ts/db-user (:id whoami))
                                                              whoami
                                                              (-> liitteet-before-delete first :id))))
        _ (valvonta-service/update-valvonta! ts/*db* energiatodistus-id true)
        _ (liite-service/delete-liite (ts/db-user (:id whoami))
                                      whoami
                                      (-> liitteet-before-delete first :id))
        liitteet-after-delete (liite-service/find-energiatodistus-liitteet
                               ts/*db*
                               whoami
                               energiatodistus-id)]
    (t/is (= 1 (count liitteet-before-delete)))
    (t/is (zero? (count liitteet-after-delete)))))
