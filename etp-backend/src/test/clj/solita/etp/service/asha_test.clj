(ns solita.etp.service.asha-test
  (:require [clojure.test :as t]
            [solita.etp.test-system :as ts]
            [clojure.java.io :as io]
            [solita.etp.service.asha :as asha-service]
            [clojure.string :as str]
            [clojure.data.codec.base64 :as b64]))

(t/use-fixtures :each ts/fixture)

(defn- handle-request [request-resource response-resource response-status]
  (fn [request]
    (t/is (= (str/trim request) (-> request-resource io/resource slurp str/trim)))
    {:body   (-> response-resource io/resource slurp)
     :status response-status}))

(t/deftest case-create-test
  (with-redefs [asha-service/make-send-requst (handle-request "asha/case-create-request.xml"
                                                              "asha/case-create-response.xml"
                                                              200)]
    (t/is (= (asha-service/case-create
               {:sender-id      "test@example.com"
                :request-id     "ETP-1"
                :classification "05.03.02"
                :service        "general"
                :name           "Asunnot Oy"
                :description    "Helsinki, Katu 1"})
             {:case-number "ARA-05.03.02-2021-31" :id 38444}))))

(t/deftest case-info-test
  (with-redefs [asha-service/make-send-requst (handle-request "asha/case-info-request.xml"
                                                              "asha/case-info-response.xml"
                                                              200)]
    (t/is (= (asha-service/case-info "test@example.com" "ETP-1" "ARA-05.03.02-2021-31")
             {:case-number    "ARA-05.03.02-2021-31"
              :id             38444
              :status         "NEW"
              :classification "05.03.02"
              :name           "Asunnot Oy"
              :description    "Helsinki, Katu 1"
              :created        "2021-03-22T10:28:13+02:00"}))))

(t/deftest case-create-without-sender-id-test
  (with-redefs [asha-service/make-send-requst (handle-request "asha/case-create-request-without-sender-id.xml"
                                                              "asha/case-create-response-without-sender-id.xml"
                                                              500)]
    (t/is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Sending xml failed with status 500\.?"
                            (asha-service/case-create
                              {:request-id     "ETP-1"
                               :classification "05.03.02"
                               :service        "general"
                               :name           "Asunnot Oy"
                               :description    "Helsinki, Katu 1"})))))

(t/deftest execute-operation-attach-contact-test
  (with-redefs [asha-service/make-send-requst (handle-request "asha/execute-operation-attach-contact-request.xml"
                                                              "asha/execute-operation-response.xml"
                                                              200)]
    (t/is (nil? (asha-service/execute-operation {:sender-id  "test@example.com"
                                                 :request-id "ETP-1"
                                                 :identity   {:case              {:number "ARA-05.03.02-2021-8"}
                                                              :processing-action {:name-identity "Vireillepano"}}
                                                 :attach     {:contact {:type       "ORGANIZATION" ;TODO: fix to use PERSON -> No enum constant fi.ys.eservice.entity.ContactType.PERSON
                                                                        :first-name "Liisa"
                                                                        :last-name  "Meikäläinen"}}})))))

(t/deftest execute-operation-processing-action-test
  (with-redefs [asha-service/make-send-requst (handle-request "asha/execute-operation-create-processing-action-request.xml"
                                                              "asha/execute-operation-response.xml"
                                                              200)]
    (t/is (nil? (asha-service/execute-operation {:sender-id         "test@example.com"
                                                 :request-id        "ETP-1"
                                                 :identity          {:case              {:number "ARA-05.03.02-2021-8"}
                                                                     :processing-action {:name-identity "Vireillepano"}}
                                                 :processing-action {:name                 "Tietopyyntö"
                                                                     :reception-date       "2021-03-02T12:54:00"
                                                                     :contacting-direction "SENT"
                                                                     :contact              {:type       "ORGANIZATION" ;TODO: fix to use PERSON -> No enum constant fi.ys.eservice.entity.ContactType.PERSON
                                                                                            :first-name "Liisa"
                                                                                            :last-name  "Meikäläinen"}}})))))

(t/deftest execute-operation-processing-action-test
  (with-redefs [asha-service/make-send-requst (handle-request "asha/execute-operation-attach-document-request.xml"
                                                              "asha/execute-operation-response.xml"
                                                              200)]
    (t/is (nil? (asha-service/execute-operation {:sender-id  "test@example.com"
                                                 :request-id "ETP-1"
                                                 :identity   {:case              {:number "ARA-05.03.02-2021-8"}
                                                              :processing-action {:name-identity "Tietopyyntö"}}
                                                 :attach     {:document [{:name    "Tietopyyntö.txt"
                                                                          :type    "Pyyntö"
                                                                          :content (String. (b64/encode (.getBytes "Test")) "UTF-8")}]}})))))

(t/deftest execute-operation-proceed-operation-test
  (with-redefs [asha-service/make-send-requst (handle-request "asha/execute-operation-proceed-operation-request.xml"
                                                              "asha/execute-operation-response.xml"
                                                              200)]
    (t/is (nil? (asha-service/proceed-operation
                  "test@example.com"
                  "ETP-1"
                  "ARA-05.03.02-2021-8"
                  "Vireillepano"
                  "Siirry käsittelyyn")))))