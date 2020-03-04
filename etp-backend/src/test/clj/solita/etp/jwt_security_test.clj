(ns solita.etp.jwt-security-test
  (:require [clojure.test :as t]
            [clojure.string :as str]
            [solita.etp.jwt-security :as jwt]))

;; Private key that was used to write the test token.
;; This is not needed in tests, but handy if you want to
;; change the token for these tests at jwt.io
(def private-key "-----BEGIN RSA PRIVATE KEY-----
MIIBOQIBAAJAUAjFSBz4vbN9AT/Z8o7RlAVnpGEjFsxNtArJZ44AEL8sefk9Mnbs
xb5hLh8PYpcCZCQ44zSP2EBO/aOVQm1zkQIDAQABAkAG88wXaJTe/cGFI0POg0OH
ZTTMnbeLmnHBixJV0YsF6UyYCfehXeb4sRqDe9eoYQxwh9ezJSuEwanIcMgIyoAx
AiEAkMampdRY8b0SqFawH/klvlLuhe8iYTyQLx5rHipFha0CIQCNhUArYrmNjmaZ
BenqOwyXgDjKm1sibN5fLnkP6cU59QIgYe4uDeBM5gB6XWp+KrKSqGJavDhdLh8U
fPr7hGUdr+UCIFMX+iV/QhOrmPIgVsgBA9OwpafQsCH2alrYnpyJVhRBAiEAjSfE
UkgX3WFgfQA3D1y9Db1rdOcDRe+ylYUMhSnpMbE=
-----END RSA PRIVATE KEY-----")

;; From jwt.io
(def public-key "-----BEGIN PUBLIC KEY-----
MFswDQYJKoZIhvcNAQEBBQADSgAwRwJAUAjFSBz4vbN9AT/Z8o7RlAVnpGEjFsxN
tArJZ44AEL8sefk9Mnbsxb5hLh8PYpcCZCQ44zSP2EBO/aOVQm1zkQIDAQAB
-----END PUBLIC KEY-----")

;; Token was created and signed with jwt.io
(def test-jwt-header "eyJraWQiOiJ0ZXN0LWtpZCIsImFsZyI6IlJTMjU2In0")
(def test-jwt-payload "eyJzdWIiOiJ0ZXN0LXN1YiIsInRva2VuX3VzZSI6ImFjY2VzcyIsInNjb3BlIjoib3BlbmlkIiwiYXV0aF90aW1lIjoxNTgzMDIwODAwLCJpc3MiOiJ0ZXN0LWlzcyIsImV4cCI6NDEwNzU0MjQwMCwiaWF0IjoxNTgzMDIwODAwLCJ2ZXJzaW9uIjoyLCJqdGkiOiJ0ZXN0LWp0aSIsImNsaWVudF9pZCI6InRlc3QtY2xpZW50X2lkIiwidXNlcm5hbWUiOiJ0ZXN0LXVzZXJuYW1lIn0")
(def test-jwt-signature "Od_tS33Kc9EqdRedxIPTxYPxJ0WSbtmMlQgRScnNIzNTQyXE1ZVR2MGRkQzmjsxKcWFIxblKzvgPgiIKVL_feg")
(def test-jwt (str/join "." [test-jwt-header
                             test-jwt-payload
                             test-jwt-signature]))

(def test-jwt-decoded-header {:kid "test-kid"
                              :alg "RS256"})
(def test-jwt-decoded-payload {:sub "test-sub",
                               :token_use "access",
                               :scope "openid",
                               :auth_time 1583020800,
                               :iss "test-iss",
                               :exp 4107542400,
                               :iat 1583020800,
                               :version 2,
                               :jti "test-jti",
                               :client_id "test-client_id",
                               :username "test-username"})

(t/deftest decode-jwt-section
  (t/is (nil? (jwt/decode-jwt-section "aaa")))
  (t/is (= (jwt/decode-jwt-section test-jwt-header)
           test-jwt-decoded-header))
  (t/is (= (jwt/decode-jwt-section test-jwt-payload)
           test-jwt-decoded-payload)))

(t/deftest decoded-jwt-test
  (t/is (nil? (jwt/decoded-jwt "aaa.bbb")))
  (t/is (nil? (jwt/decoded-jwt (str test-jwt ".ddd"))))
  (t/is (= (jwt/decoded-jwt test-jwt)
           {:header test-jwt-decoded-header
            :payload test-jwt-decoded-payload
            :signature test-jwt-signature})))
