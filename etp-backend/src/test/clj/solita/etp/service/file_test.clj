(ns solita.etp.service.file-test
  (:require [clojure.java.io :as io]
            [clojure.test :as t]
            [solita.etp.test-system :as ts]
            [solita.etp.service.file :as service]))

(t/use-fixtures :each ts/fixture)

(def file-infos
  [{:id "id-1"
    :filename "user.clj"
    :path "src/main/clj/user.clj"
    :bytes (-> "src/main/clj/user.clj" io/input-stream .readAllBytes)}
   {:id "id-2"
    :filename "deps.edn"
    :path "deps.edn"
    :bytes (-> "deps.edn" io/input-stream .readAllBytes)}])

(t/deftest file->byte-array-test
  (doseq [file-info file-infos]
    (t/is (= (-> file-info :path io/file service/file->byte-array type str)
             "class [B"))))

(t/deftest add-file-and-find-test
  (service/add-file-from-file ts/*db*
                              (-> file-infos first :id)
                              (-> file-infos first :path io/file))
  (service/add-file-from-input-stream ts/*db*
                                      (-> file-infos second :id)
                                      (-> file-infos second :filename)
                                      (-> file-infos
                                          second
                                          :path
                                          io/input-stream))
  (doseq [file-info file-infos
          :let [{:keys [filename content]} (service/find-file ts/*db*
                                                              (:id file-info))]]
    (t/is (= (:filename file-info) filename))
    (t/is (true? (instance? java.io.InputStream content)))
    (t/is (= (into [] (:bytes file-info))
             (into [] (.readAllBytes content)))))
  (t/is (nil? (service/find-file ts/*db* "nonexisting"))))
