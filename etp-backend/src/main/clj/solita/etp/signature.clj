(ns solita.etp.signature
  (:import
   (java.io FileInputStream InputStreamReader StringReader)
   (java.util Base64)
   (org.bouncycastle.asn1.pkcs PrivateKeyInfo)
   (org.bouncycastle.crypto.digests SHA1Digest)
   (org.bouncycastle.crypto.signers RSADigestSigner)
   (org.bouncycastle.crypto.util PrivateKeyFactory)
   (org.bouncycastle.openssl PEMKeyPair PEMParser)))

(defn- reader->parsed-pem [reader]
  (-> reader
      PEMParser.
      .readObject))

(defn- path->parsed-pem [path]
  (with-open [stream (FileInputStream. path)]
    (-> stream
        (InputStreamReader. "UTF-8")
        reader->parsed-pem)))

(defn- str->parsed-pem [s]
  (-> s
      StringReader.
      reader->parsed-pem))

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

(defn load-private-key-str [pem-str]
  (-> pem-str
      str->parsed-pem
      pem->key))

(defn load-private-key-from [path]
  (-> path
      path->parsed-pem
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
