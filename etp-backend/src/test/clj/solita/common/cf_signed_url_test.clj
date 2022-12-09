(ns solita.common.cf-signed-url-test
  (:require [clojure.test :as t]
            [clojure.java.io :as io]
            [solita.common.cf-signed-url :as signed-url]))

(t/deftest signed-url-roundtrip-test
  (let [private-key (-> "cf-signed-url/example.key.pem" io/resource slurp signed-url/pem-string->private-key)
        public-key (-> "cf-signed-url/example.pub.pem" io/resource slurp signed-url/pem-string->public-key)
        key-pair-id "L3G17K3Y"
        base-url "https://energiatodistusrekisteri.fi"
        intended-ip-address "10.0.7.10"
        other-ip-address "10.0.17.1"
        time-issued (signed-url/unix-time)
        expires (+ time-issued 60)
        time-late-test (+ expires 10)
        optimistic-expiration (+ time-late-test 10)
        url (signed-url/url->signed-url base-url
                                        expires
                                        intended-ip-address
                                        {:key-pair-id key-pair-id
                                         :private-key private-key})
        verify-keys {:key-pair-id key-pair-id
                     :public-key public-key}]

    ;; Positive verification
    (t/is (nil? (signed-url/signed-url-problem-now url
                                                   intended-ip-address
                                                   verify-keys)))

    ;; Verify after expiration
    (t/is (= :expired-url
             (signed-url/signed-url-problem url
                                            intended-ip-address
                                            time-late-test
                                            verify-keys)))

    ;; Check other ip adderss
    ;; Positive verification
    (t/is (= :invalid-ip-address
             (signed-url/signed-url-problem-now url
                                                other-ip-address
                                                verify-keys)))

    ;; Tamper by adding extra parameters For now this is a format
    ;; error, since we do not expect to use query parameters at all
    (t/is (= :format
             (signed-url/signed-url-problem-now (str url "&asd=basd")
                                                intended-ip-address
                                                verify-keys)))))
