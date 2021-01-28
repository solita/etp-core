(ns solita.common.sftp-test
  (:require [clojure.test :as t]
            [solita.common.sftp :as sftp]))

(def host "localhost")
(def port 2222)
(def username "etp")
(def password "etp")
(def known-hosts-path "known_hosts")
(def src-path "deps.edn")

(t/deftest sftp-test
  (let [tmp-dir (.toString (java.util.UUID/randomUUID))
        dst-path (str tmp-dir "/" src-path)
        connection (sftp/connect! host port username password known-hosts-path)]
    (t/is (sftp/connected? connection))
    (t/is (-> (sftp/list-files connection) (contains? tmp-dir) not))
    (sftp/make-directory! connection tmp-dir)
    (sftp/make-directory! connection tmp-dir)
    (t/is (-> (sftp/list-files connection) (contains? tmp-dir)))
    (t/is (-> (sftp/list-files connection tmp-dir) (contains? src-path) not))
    (sftp/upload! connection src-path dst-path)
    (t/is (-> (sftp/list-files connection tmp-dir) (contains? src-path)))
    (sftp/delete! connection dst-path)
    (sftp/delete! connection dst-path)
    (t/is (-> (sftp/list-files connection tmp-dir) (contains? src-path) not))
    (sftp/delete! connection tmp-dir true)
    (t/is (-> (sftp/list-files connection) (contains? tmp-dir) not))
    (sftp/disconnect! connection)
    (t/is (false? (sftp/connected? connection)))))
