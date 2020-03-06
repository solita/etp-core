(ns solita.etp.jwt-security-test
  (:require [clojure.test :as t]
            [clojure.java.io :as io]
            [buddy.core.keys :as keys]
            [solita.etp.service.json :as json]
            [solita.etp.jwt-security :as jwt-security]))

;; It would be possible to just create a jwt for tests with buddy. However,
;; would that actually test anything really? For this reason the token in
;; these tests has been created with jwt.io.

;; Private key that was used to write the test token. This is not needed in
;; tests, but handy if you want to change the token for these tests at jwt.io.
(def private-key "-----BEGIN RSA PRIVATE KEY-----
MIIBOQIBAAJAUAjFSBz4vbN9AT/Z8o7RlAVnpGEjFsxNtArJZ44AEL8sefk9Mnbs
xb5hLh8PYpcCZCQ44zSP2EBO/aOVQm1zkQIDAQABAkAG88wXaJTe/cGFI0POg0OH
ZTTMnbeLmnHBixJV0YsF6UyYCfehXeb4sRqDe9eoYQxwh9ezJSuEwanIcMgIyoAx
AiEAkMampdRY8b0SqFawH/klvlLuhe8iYTyQLx5rHipFha0CIQCNhUArYrmNjmaZ
BenqOwyXgDjKm1sibN5fLnkP6cU59QIgYe4uDeBM5gB6XWp+KrKSqGJavDhdLh8U
fPr7hGUdr+UCIFMX+iV/QhOrmPIgVsgBA9OwpafQsCH2alrYnpyJVhRBAiEAjSfE
UkgX3WFgfQA3D1y9Db1rdOcDRe+ylYUMhSnpMbE=
-----END RSA PRIVATE KEY-----")

(def public-key-str "-----BEGIN PUBLIC KEY-----
MFswDQYJKoZIhvcNAQEBBQADSgAwRwJAUAjFSBz4vbN9AT/Z8o7RlAVnpGEjFsxN
tArJZ44AEL8sefk9Mnbsxb5hLh8PYpcCZCQ44zSP2EBO/aOVQm1zkQIDAQAB
-----END PUBLIC KEY-----")
(def wrong-public-key-str "-----BEGIN PUBLIC KEY-----
MFswDQYJKoZIhvcNAQEBBQADSgAwRwJAezq/MsnCf6AT0uLxB5amXZOk4ijsOWS/
zI6qYxXKEuxvD4MQFVc90/nB+nNLVQjDCfY91p/Ty0VjPIenVMV99QIDAQAB
-----END PUBLIC KEY-----")

(def public-key (keys/str->public-key public-key-str))
(def wrong-public-key (keys/str->public-key wrong-public-key-str))

;; Tokens were created at jwt.io
(def ok-jwt "eyJraWQiOiJ0ZXN0LWtpZCIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJ0ZXN0LXN1YiIsInRva2VuX3VzZSI6ImFjY2VzcyIsInNjb3BlIjoib3BlbmlkIiwiYXV0aF90aW1lIjoxNTgzMDIwODAwLCJpc3MiOiJ0ZXN0LWlzcyIsImV4cCI6NDEwNzU0MjQwMCwiaWF0IjoxNTgzMDIwODAwLCJ2ZXJzaW9uIjoyLCJqdGkiOiJ0ZXN0LWp0aSIsImNsaWVudF9pZCI6InRlc3QtY2xpZW50X2lkIiwidXNlcm5hbWUiOiJ0ZXN0LXVzZXJuYW1lIn0.BqruGXbNKppJdaaUU0QD5pVMGLHm_o9-XVALfN0p8OJgZOoFvWpUi4a-LHOsQAM-cbO5XJ1ex_jyOXdam5pV9Q")
(def expired-jwt "eyJraWQiOiJ0ZXN0LWtpZCIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJ0ZXN0LXN1YiIsInRva2VuX3VzZSI6ImFjY2VzcyIsInNjb3BlIjoib3BlbmlkIiwiYXV0aF90aW1lIjoxNTgzMDIwODAwLCJpc3MiOiJ0ZXN0LWlzcyIsImV4cCI6MTU4MzAyMDgwMCwiaWF0IjoxNTgzMDIwODAwLCJ2ZXJzaW9uIjoyLCJqdGkiOiJ0ZXN0LWp0aSIsImNsaWVudF9pZCI6InRlc3QtY2xpZW50X2lkIiwidXNlcm5hbWUiOiJ0ZXN0LXVzZXJuYW1lIn0.HLzZONfkg9f7EMSWnDsjaMAkPWpgkSp1Om0Y8_PLgeIlQtKncBBvxOTNlQgn6FJx4nB7pe4vwZ3u1qJoC82dcw")

(def ok-jwt-payload {:sub "test-sub",
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

(def jwks (-> ".well-known/jwks.json" io/resource slurp json/read-value))

(t/deftest trusted-iss->jwks-url-test
  (t/is (= (jwt-security/trusted-iss->jwks-url nil)
           "/.well-known/jwks.json"))
  (t/is (= (jwt-security/trusted-iss->jwks-url "https://example.com")
           "https://example.com/.well-known/jwks.json")))

(t/deftest public-key-from-jwks-test
  (t/is (nil? (jwt-security/public-key-from-jwks jwks "nonexisting-kid")))
  (t/is (= (-> jwks
               (jwt-security/public-key-from-jwks "test-kid")
               .getPublicExponent)
           65537)))

(t/deftest verified-jwt-test
  (t/is (nil? (jwt-security/verified-jwt-payload nil public-key)))
  (t/is (nil? (jwt-security/verified-jwt-payload ok-jwt nil)))
  (t/is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Token is expired \(1583020800\)"
                          (jwt-security/verified-jwt-payload expired-jwt public-key)))

  (t/is (= (jwt-security/verified-jwt-payload ok-jwt public-key)
           ok-jwt-payload)))

(t/deftest alb-headers-test
  (t/is (nil? (jwt-security/alb-headers
               {:headers {"x-amzn-oidc-identity" "123"}})))
  (t/is (nil? (jwt-security/alb-headers
               {:headers {"x-amzn-oidc-accesstoken" "abc"}})))
  (t/is (= (jwt-security/alb-headers
            {:headers {"x-amzn-oidc-identity" "123"
                       "x-amzn-oidc-accesstoken" "abc"}})
           {:id "123"
            :jwt "abc"})))
