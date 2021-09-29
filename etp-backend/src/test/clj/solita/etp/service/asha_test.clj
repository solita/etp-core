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

(t/deftest open-case-test
  (binding [asha-service/make-send-request! (handle-request "asha/case-create-request.xml"
                                                            "asha/case-create-response.xml"
                                                             200)]
    (t/is (= (asha-service/open-case!
               {:sender-id      "test@example.com"
                :request-id     "ETP-1"
                :classification "05.03.02"
                :service        "general"
                :name           "Asunnot Oy"
                :description    "Helsinki, Katu 1"})
             "ARA-05.03.02-2021-31"))))

(t/deftest case-info-test
  (binding [asha-service/make-send-request! (handle-request "asha/case-info-request.xml"
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

(t/deftest action-info-test
  (binding [asha-service/make-send-request! (handle-request "asha/action-info-request.xml"
                                                            "asha/action-info-response.xml"
                                                            200)]
    (t/is (= (asha-service/action-info "test@example.com" "ETP-1" "ARA-05.03.02-2021-8" "Vireillepano")
             {:processing-action
                        {:object-class         "ProcessingAction",
                         :id                   578877,
                         :version              8,
                         :contacting-direction "NONE",
                         :name                 "Vireillepano",
                         :description          "Luodaan uusi asia.",
                         :status               "READY",
                         :created              "2021-03-25T12:47:08+02:00"},
              :assignee "FFAB23AE-E49F-4E3B-98D9-40C3DE0E4B46",
              :queue    24,
              :selected-decision
                        {:decision "Siirry käsittelyyn",
                         :next-processing-action
                                   {:object-class         "ProcessingAction",
                                    :id                   579176,
                                    :version              1,
                                    :contacting-direction "NONE",
                                    :name                 "Käsittely",
                                    :description
                                                          "Asian käsittelyn toimenpiteitä, esim. lausunnon laatiminen, selvityksen tekeminen, päätöksen valmistelu",
                                    :status               "NEW",
                                    :created              "2021-03-25T14:01:07+02:00"}}}))))

(t/deftest case-create-without-sender-id-test
  (binding [asha-service/make-send-request! (handle-request "asha/case-create-request-without-sender-id.xml"
                                                            "asha/case-create-response-without-sender-id.xml"
                                                            500)]
    (t/is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Sending xml failed with status 500\.?"
                            (asha-service/open-case!
                              {:request-id     "ETP-1"
                               :classification "05.03.02"
                               :service        "general"
                               :name           "Asunnot Oy"
                               :description    "Helsinki, Katu 1"})))))

(t/deftest execute-operation-attach-contact-test
  (binding [asha-service/make-send-request! (handle-request "asha/execute-operation-attach-contact-request.xml"
                                                            "asha/execute-operation-response.xml"
                                                            200)]
    (t/is (nil? (asha-service/execute-operation! {:sender-id  "test@example.com"
                                                  :request-id "ETP-1"
                                                  :identity   {:case              {:number "ARA-05.03.02-2021-8"}
                                                               :processing-action {:name-identity "Vireillepano"}}
                                                  :attach     {:contact {:type       "PERSON"
                                                                         :first-name "Liisa"
                                                                         :last-name  "Meikäläinen"}}})))))

(t/deftest execute-operation-processing-action-test
  (binding [asha-service/make-send-request! (handle-request "asha/execute-operation-create-processing-action-request.xml"
                                                            "asha/execute-operation-response.xml"
                                                            200)]
    (t/is (nil? (asha-service/execute-operation! {:sender-id         "test@example.com"
                                                  :request-id        "ETP-1"
                                                  :identity          {:case              {:number "ARA-05.03.02-2021-8"}
                                                                      :processing-action {:name-identity "Vireillepano"}}
                                                  :processing-action {:name                 "Tietopyyntö"
                                                                      :reception-date       "2021-03-02T12:54:00"
                                                                      :contacting-direction "SENT"
                                                                      :contact              {:type       "PERSON"
                                                                                             :first-name "Liisa"
                                                                                             :last-name  "Meikäläinen"}}})))))

(t/deftest execute-operation-attach-document-test
  (binding [asha-service/make-send-request! (handle-request "asha/execute-operation-attach-document-request.xml"
                                                            "asha/execute-operation-response.xml"
                                                            200)]
    (t/is (nil? (asha-service/execute-operation! {:sender-id  "test@example.com"
                                                  :request-id "ETP-1"
                                                  :identity   {:case              {:number "ARA-05.03.02-2021-8"}
                                                               :processing-action {:name-identity "Tietopyyntö"}}
                                                  :attach     {:document [{:name    "Tietopyyntö.txt"
                                                                           :type    "Pyyntö"
                                                                           :content (String. (b64/encode (.getBytes "Test")) "UTF-8")}]}})))))

(t/deftest execute-operation-proceed-operation-test
  (binding [asha-service/make-send-request! (handle-request "asha/execute-operation-proceed-operation-request.xml"
                                                            "asha/execute-operation-response.xml"
                                                            200)]
    (t/is (nil? (asha-service/proceed-operation!
                  "test@example.com"
                  "ETP-1"
                  "ARA-05.03.02-2021-8"
                  "Vireillepano"
                  "Siirry käsittelyyn")))))