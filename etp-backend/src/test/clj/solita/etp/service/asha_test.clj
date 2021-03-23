(ns solita.etp.service.asha-test
  (:require [clojure.test :as t]
            [solita.etp.test-system :as ts]
            [clojure.java.io :as io]
            [solita.etp.service.asha :as asha-service]
            [clojure.string :as str]))

(t/use-fixtures :each ts/fixture)

(defn- handle-request [request-resource response-resource]
  (fn [request]
    (t/is (= (str/trim request) (-> request-resource io/resource slurp str/trim)))
    (-> response-resource io/resource slurp)))

(t/deftest case-create-test
  (with-redefs [asha-service/send-request (handle-request "asha/case-create-request.xml" "asha/case-create-response.xml")]
    (t/is (= (asha-service/case-create
               {:handler        "test@example.com"
                :request-id     "ETP-1"
                :classification "04.07"
                :service        "general"
                :name           "Asunnot Oy"
                :description    "Helsinki, Katu 1"})
             {:case-number "ARA-04.07-2021-31" :id 38444}))))