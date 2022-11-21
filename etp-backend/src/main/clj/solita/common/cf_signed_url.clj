(ns solita.common.cf-signed-url
  (:require [solita.etp.service.json :as json])
  (:import
   (java.io FileInputStream InputStreamReader StringReader)
   (java.util Base64)
   (org.bouncycastle.asn1.pkcs PrivateKeyInfo)
   (org.bouncycastle.crypto.digests SHA1Digest)
   (org.bouncycastle.crypto.signers RSADigestSigner)
   (org.bouncycastle.crypto.util PrivateKeyFactory)
   (org.bouncycastle.openssl PEMKeyPair PEMParser)))

;; Namespace for producing a signed URL for CloudFront canned policy, as defined at
;; https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-creating-signed-url-canned-policy.html

(defn str->parsed-pem [s]
  (-> s
      StringReader.
      PEMParser.
      .readObject))

(defn- pem->key [parsed-pem]
  ;; Not sure why it is defined that way, but the type of object
  ;; returned by PEMParser.readObject seems to vary depending on quite
  ;; inconsequential things. For example, seems that PKCS1 formatted
  ;; private key is returned as PEMKeyPair, while PKCS8 formatted
  ;; private key produces just a PrivateKeyInfo object
  (cond
    (instance? PEMKeyPair parsed-pem)
    (-> parsed-pem
        .getPrivateKeyInfo
        PrivateKeyFactory/createKey)
    (instance? PrivateKeyInfo parsed-pem)
    (-> parsed-pem
        PrivateKeyFactory/createKey)
    :else
    (PrivateKeyFactory/createKey parsed-pem)))

(defn pem-string->private-key [pem-str]
  (-> pem-str
      str->parsed-pem
      pem->key))

(defn- bytes->base64 [b]
  (let [encoder (Base64/getEncoder)]
    (.encode encoder b)))

(defn base64->querystring-safe-str [s]
  (-> s
      (.replace \+ \-)
      (.replace \= \_)
      (.replace \/ \~)))

(defn sign-document [document key]
  (let [signer (RSADigestSigner. (SHA1Digest.))
        bytes (.getBytes document)]
    (.init signer true key)
    (.update signer bytes 0 (count bytes))
    (-> signer
        .generateSignature
        bytes->base64
        String.
        base64->querystring-safe-str)))

(defn unix-time []
  (quot (System/currentTimeMillis) 1000))

(defn policy-document [url end-time]
  {"Statement" [{"Resource" url
                 "Condition" {"DateLessThan" {"AWS:EpochTime" end-time}}}]})

(defn url->signed-url [url expires {:keys [key-pair-id private-key]}]
  (str url
       "?Expires=" expires
       "&Signature=" (sign-document (-> (policy-document url expires)
                                        json/write-value-as-string) private-key)
       "&Key-Pair-Id=" key-pair-id))
