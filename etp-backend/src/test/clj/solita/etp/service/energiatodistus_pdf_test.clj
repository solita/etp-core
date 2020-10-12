(ns solita.etp.service.energiatodistus-pdf-test
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.test :as t]
            [schema-generators.generators :as g]
            [solita.etp.test-system :as ts]
            [solita.common.xlsx :as xlsx]
            [solita.etp.schema.energiatodistus :as schema]
            [solita.etp.service.energiatodistus-pdf :as service]
            [solita.etp.service.energiatodistus :as energiatodistus-service]
            [solita.etp.service.energiatodistus-test :as energiatodistus-test]
            [solita.common.certificates-test :as certificates-test]))

(t/use-fixtures :each ts/fixture)

(def energiatodistus-2013 #(-> (energiatodistus-test/generate-energiatodistus-2013)
                               (assoc :versio 2013)))

(def energiatodistus-2018 #(-> (energiatodistus-test/generate-energiatodistus-2018)
                               (assoc :versio 2018)))

(def energiatodistukset #(pcalls energiatodistus-2018 energiatodistus-2013))

(def sis-kuorma-data {:henkilot {:kayttoaste 0.2 :lampokuorma 1}
                      :kuluttajalaitteet {:kayttoaste 0.3 :lampokuorma 1}
                      :valaistus {:kayttoaste 0.3 :lampokuorma 2}})

(t/deftest sis-kuorma-test
  (let [sis-kuorma (service/sis-kuorma {:lahtotiedot {:sis-kuorma
                                                      sis-kuorma-data}})]
    (t/is (= sis-kuorma [[0.2 {:henkilot 1}]
                         [0.3 {:kuluttajalaitteet 1 :valaistus 2}]]))))

(t/deftest format-number-test
  (t/is (= "12,346" (service/format-number 12.34567 3 false)))
  (t/is (= "0,84" (service/format-number 0.8449 2 false)))
  (t/is (= "100 %" (service/format-number 1 0 true)))
  (t/is (= "12,346 %" (service/format-number 0.1234567 3 true))))

(t/deftest fill-xlsx-template-test
  (doseq [energiatodistus (energiatodistukset)]
    (let [path (service/fill-xlsx-template energiatodistus "fi" false)
          file (-> path io/input-stream)
          loaded-xlsx (xlsx/load-xlsx file)
          sheet-0 (xlsx/get-sheet loaded-xlsx 0)]
      (t/is (str/ends-with? path ".xlsx"))
      (t/is (-> path io/as-file .exists true?))
      (t/is (= (:id energiatodistus)
               (xlsx/get-cell-value-at sheet-0 (case (:versio energiatodistus)
                                                 2013 "I17"
                                                 2018 "K16"))))
      (io/delete-file path))))

(t/deftest xlsx->pdf-test
  (let [file-path (service/xlsx->pdf (str "src/main/resources/"
                                          "energiatodistus-2018-fi.xlsx"))]
    (t/is (str/ends-with? file-path ".pdf"))
    (t/is (-> file-path io/as-file .exists true?))
    (io/delete-file file-path)))

(t/deftest generate-pdf-as-file-test
  (doseq [energiatodistus (energiatodistukset)]
    (let [file-path (service/generate-pdf-as-file energiatodistus "sv" true)]
      (t/is (-> file-path io/as-file .exists))
      (io/delete-file file-path))))

(t/deftest pdf-file-id-test
  (t/is (nil? (service/pdf-file-id nil "fi")))
  (t/is (= (service/pdf-file-id 12345 "fi") "energiatodistus-12345-fi")))

(t/deftest do-when-signing-test
  (let [f (constantly true)]
    (t/is (= (service/do-when-signing {:tila-id 0 } f)
             :not-in-signing))
    (t/is (true? (service/do-when-signing {:tila-id 1} f)))

    (t/is (= (service/do-when-signing {:tila-id 2} f)
             :already-signed))))

(t/deftest find-energiatodistus-digest-test
  (let [laatija-id (energiatodistus-test/add-laatija!)
        id (energiatodistus-test/add-energiatodistus!
             (energiatodistus-test/generate-energiatodistus-2018-complete)
             laatija-id)
        whoami {:id laatija-id}]
    (t/is (= (service/find-energiatodistus-digest ts/*db* ts/*aws-s3-client* id "fi")
             :not-in-signing))
    (energiatodistus-service/start-energiatodistus-signing! ts/*db* whoami id)
    (t/is (contains? (service/find-energiatodistus-digest ts/*db* ts/*aws-s3-client* id "fi")
                     :digest))
    (energiatodistus-service/end-energiatodistus-signing! ts/*db* whoami id)
    (t/is (= (service/find-energiatodistus-digest ts/*db* ts/*aws-s3-client* id "fi")
             :already-signed))))

(t/deftest comparable-name-test
  (t/is (= "abc" (service/comparable-name "abc")))
  (t/is (= "aeiouao" (service/comparable-name "á, é, í, ó, ú. ä ö"))))

(t/deftest validate-surname!-test
  (t/is (thrown? clojure.lang.ExceptionInfo
                 (service/validate-surname! "Meikäläinen"
                                            certificates-test/test-cert-str)))
  (t/is (nil? (service/validate-surname! "Specimen-POtex"
                                         certificates-test/test-cert-str))))

(t/deftest sign-energiatodistus-test
  (let [laatija-id (energiatodistus-test/add-laatija!)
        id (energiatodistus-test/add-energiatodistus!
             (energiatodistus-test/generate-energiatodistus-2018-complete)
             laatija-id)
        whoami {:id laatija-id}]
    (t/is (= (service/sign-energiatodistus-pdf ts/*db* ts/*aws-s3-client* whoami id "fi" nil)
             :not-in-signing))
    (energiatodistus-service/start-energiatodistus-signing! ts/*db* whoami id)

    ;; Is it possible to somehow create a valid signature and chain for testing
    ;; the success case?

    (energiatodistus-service/end-energiatodistus-signing! ts/*db* whoami id)
    (t/is (= (service/sign-energiatodistus-pdf ts/*db* ts/*aws-s3-client* whoami id "fi" nil)
             :already-signed))))
