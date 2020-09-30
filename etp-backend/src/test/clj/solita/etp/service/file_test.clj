(ns solita.etp.service.file-test
  (:require [clojure.java.io :as io]
            [clojure.test :as t]
            [solita.etp.test-system :as ts]
            [solita.etp.service.file :as service]))

(t/use-fixtures :each ts/fixture)

(def file-info-1 {:id "id-1"
                  :filename "some-text.txt"
                  :bytes (byte-array (map byte "Some text"))})

(def file-info-2 {:id "id-2"
                  :filename "user.clj"
                  :path "src/main/clj/user.clj"
                  :bytes (-> "src/main/clj/user.clj"
                             io/input-stream
                             .readAllBytes)})

(def file-info-3 {:id "id-3"
                  :filename "deps.edn"
                  :path "deps.edn"
                  :bytes (-> "deps.edn" io/input-stream .readAllBytes)})

(t/deftest file->byte-array-test
  (doseq [file-info [file-info-2 file-info-3]]
    (t/is (= (-> file-info :path io/file service/file->byte-array type str)
             "class [B"))))

(t/deftest upsert-file-and-find-test
  (println "dd"  ts/*aws-s3-client*)
  (service/upsert-file-from-bytes ts/*aws-s3-client*
                                  (:id file-info-1)
                                  (:filename file-info-1)
                                  (:bytes file-info-1))
  (service/upsert-file-from-file ts/*aws-s3-client*
                                 (:id file-info-2)
                                 (-> file-info-2 :path io/file))
  (service/upsert-file-from-input-stream ts/*aws-s3-client*
                                         (:id file-info-3)
                                         (:filename file-info-3)
                                         (-> file-info-3 :path io/input-stream))
  (doseq [file-info [file-info-1 file-info-2 file-info-3]
          :let [{:keys [filename content]} (service/find-file ts/*aws-s3-client* (:id file-info))]]
    (t/is (= (:filename file-info) filename))
    (t/is (true? (instance? java.io.InputStream content)))
    (t/is (= (into [] (:bytes file-info))
             (into [] (.readAllBytes content)))))
  (t/is (nil? (service/find-file ts/*aws-s3-client* "nonexisting"))))

(t/deftest rewrite-test
  (let [id (:id file-info-1)]
    (service/upsert-file-from-bytes ts/*aws-s3-client*
                                    id
                                    (:filename file-info-1)
                                    (:bytes file-info-1))
    (service/upsert-file-from-input-stream ts/*aws-s3-client*
                                           id
                                           (:filename file-info-2)
                                           (-> file-info-2 :path io/input-stream))
    (let [{:keys [filename content]} (service/find-file ts/*aws-s3-client* id)]
      (t/is (= (:filename file-info-2) filename))
      (t/is (true? (instance? java.io.InputStream content)))
      (t/is (= (into [] (:bytes file-info-2))
               (into [] (.readAllBytes content)))))))
