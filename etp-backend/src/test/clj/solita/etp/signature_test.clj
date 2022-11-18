(ns solita.etp.signature-test
  (:require [clojure.test :as t]
            [solita.etp.signature :as signature]))

(t/deftest signature-safe-base64-test
  (t/is (= "asd-basd_foo~bar"
           (signature/base64->querystring-safe-str "asd+basd=foo/bar")))
  (t/is (= "asd---basd_foo~bar"
           (signature/base64->querystring-safe-str "asd+++basd=foo/bar")))
  (t/is (= "asd-_-basd_foo~bar"
           (signature/base64->querystring-safe-str "asd+=+basd=foo/bar"))))
