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
            [solita.etp.service.energiatodistus-test :as energiatodistus-test]))

(t/use-fixtures :each ts/fixture)

(def energiatodistus (g/generate
                      schema/EnergiatodistusSave2018
                      energiatodistus-test/energiatodistus-generators))

(def sis-kuorma-data {:henkilot {:kayttoaste 0.2 :lampokuorma 1}
                      :kuluttajalaitteet {:kayttoaste 0.3 :lampokuorma 1}
                      :valaistus {:kayttoaste 0.3 :lampokuorma 2}})

(t/deftest sis-kuorma-test
  (let [sis-kuorma (service/sis-kuorma {:lahtotiedot {:sis-kuorma
                                                      sis-kuorma-data}})]
    (t/is (= sis-kuorma [[0.2 {:henkilot 1}]
                         [0.3 {:kuluttajalaitteet 1 :valaistus 2}]]))))

(t/deftest fill-xlsx-template-test
  (let [path (service/fill-xlsx-template energiatodistus false)
        file (-> path io/input-stream)
        loaded-xlsx (xlsx/load-xlsx file)
        sheet-0 (xlsx/get-sheet loaded-xlsx 0)]
    (t/is (str/ends-with? path ".xlsx"))
    (t/is (-> path io/as-file .exists true?))
    (t/is (= (-> energiatodistus :perustiedot :nimi)
             (xlsx/get-cell-value-at sheet-0 "K7")))
    (io/delete-file path)))

(t/deftest xlsx->pdf-test
  (let [file-path (service/xlsx->pdf (str "src/main/resources/"
                                          service/xlsx-template-path))]
    (t/is (str/ends-with? file-path ".pdf"))
    (t/is (-> file-path io/as-file .exists true?))
    (io/delete-file file-path)))

(t/deftest generate-pdf-as-file-test
  (let [file-path (service/generate-pdf-as-file energiatodistus true)]
    (t/is (-> file-path io/as-file .exists))
    (io/delete-file file-path)))

(t/deftest pdf-file-id-test
  (t/is (nil? (service/pdf-file-id nil)))
  (t/is (= (service/pdf-file-id 12345) "energiatodistus-12345")))

(t/deftest do-when-signing-test
  (let [f (constantly true)]
    (t/is (= (service/do-when-signing {:tila-id 0 } f)
             :not-in-signing))
    (t/is (true? (service/do-when-signing {:tila-id 1} f)))

    (t/is (= (service/do-when-signing {:tila-id 2} f)
             :already-signed))))

(t/deftest find-energiatodistus-digest-test
  (let [laatija-id (energiatodistus-test/add-laatija!)
        id (energiatodistus-test/add-energiatodistus! energiatodistus laatija-id)
        whoami {:id laatija-id}]
    (t/is (= (service/find-energiatodistus-digest ts/*db* id)
             :not-in-signing))
    (energiatodistus-service/start-energiatodistus-signing! ts/*db* whoami id)
    (t/is (contains? (service/find-energiatodistus-digest ts/*db* id)
                     :digest))
    (energiatodistus-service/end-energiatodistus-signing! ts/*db* whoami id)
    (t/is (= (service/find-energiatodistus-digest ts/*db* id)
             :already-signed))))

(t/deftest sign-energiatodistus-test
  (let [laatija-id (energiatodistus-test/add-laatija!)
        id (energiatodistus-test/add-energiatodistus! energiatodistus laatija-id)
        whoami {:id laatija-id}]
    (t/is (= (service/sign-energiatodistus-pdf ts/*db* id nil)
             :not-in-signing))
    (energiatodistus-service/start-energiatodistus-signing! ts/*db* whoami id)

    ;; Is it possible to somehow create a valid signature and chain for testing
    ;; the success case?

    (energiatodistus-service/end-energiatodistus-signing! ts/*db* whoami id)
    (t/is (= (service/sign-energiatodistus-pdf ts/*db* id nil)
             :already-signed))))
