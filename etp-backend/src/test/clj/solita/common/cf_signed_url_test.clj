(ns solita.common.cf-signed-url-test
  (:require [clojure.test :as t]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [solita.common.cf-signed-url :as signed-url]
            [solita.etp.service.json :as json])
  (:import (java.util Base64)
           (org.bouncycastle.asn1.x509 SubjectPublicKeyInfo)
           (org.bouncycastle.crypto.digests SHA1Digest)
           (org.bouncycastle.crypto.signers RSADigestSigner)
           (org.bouncycastle.crypto.util PublicKeyFactory)))

(defn pem->public-key [parsed-pem]
  (cond
    (instance? SubjectPublicKeyInfo parsed-pem)
    (-> parsed-pem
        PublicKeyFactory/createKey)
    :else
    (throw (ex-info "unexpected kind of PEM"))))

(defn pem-string->public-key [pem-text]
  (-> pem-text
      signed-url/str->parsed-pem
      pem->public-key))

(defn split-at-char [s c]
  (let [index-of-c (str/index-of s c)]
    (if (nil? index-of-c)
      [s ""]
      [(.substring s 0 index-of-c)
       (.substring s (inc index-of-c))])))

(defn querystring-safe-base64->bytes [s]
  (let [decoder (Base64/getDecoder)
        base64 (-> s
                   (.replace \- \+)
                   (.replace \_ \=)
                   (.replace \~ \/))]
    (.decode decoder base64)))

(defn signed-url->components [surl]
  (let [[base-url params-text] (split-at-char surl \?)
        params (into {}
                     (-> params-text
                         (str/split #"&")
                         (->> (map #(str/split % #"=")))))]
    {:base-url base-url
     :expires (-> params (get "Expires") Integer/parseInt)
     :signature (-> params
                    (get "Signature")
                    querystring-safe-base64->bytes)
     :key-pair-id (-> params (get "Key-Pair-Id"))}))

(defn assert-valid-signed-url! [signed-url expected-key-pair-id public-key]
  (let [{:keys [base-url expires key-pair-id signature]} (signed-url->components signed-url)
        policy-document-bytes (-> (signed-url/policy-document base-url expires)
                                  json/write-value-as-string
                                  .getBytes)
        signer (RSADigestSigner. (SHA1Digest.))]
    (.init signer false public-key)
    (.update signer policy-document-bytes 0 (count policy-document-bytes))
    (assert (.verifySignature signer signature))
    (assert (= expected-key-pair-id key-pair-id))))

(t/deftest signed-url-roundtrip-test
  (let [private-key (-> "cf-signed-url/example.key.pem" io/resource slurp signed-url/pem-string->private-key)
        public-key (-> "cf-signed-url/example.pub.pem" io/resource slurp pem-string->public-key)
        key-pair-id "L3G17K3Y"
        base-url "https://energiatodistusrekisteri.fi"
        expires (+ 60 (signed-url/unix-time))
        url (signed-url/url->signed-url base-url
                                        expires
                                        {:key-pair-id key-pair-id
                                         :private-key private-key})
        url-components (signed-url->components url)]
    ;; This pretty much just tests the deserialization
    (t/is (= base-url (:base-url url-components)))
    (t/is (= expires (:expires url-components)))
    (t/is (= key-pair-id (:key-pair-id url-components)))
    (assert-valid-signed-url! url key-pair-id public-key)))
