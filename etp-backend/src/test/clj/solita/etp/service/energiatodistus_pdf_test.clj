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

(t/deftest fill-xlsx-template-test
  (let [path (service/fill-xlsx-template energiatodistus)
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

(t/deftest generate-test
  (let [file-path (service/generate energiatodistus)]
    (t/is (-> file-path io/as-file .exists))
    (io/delete-file file-path)))

(t/deftest pdf-file-id-test
  (t/is (nil? (service/pdf-file-id nil)))
  (t/is (= (service/pdf-file-id 12345) "energiatodistus-12345")))

(t/deftest find-energiatodistus-digest-test
  (let [id (energiatodistus-test/add-energiatodistus! energiatodistus)]
    (t/is (= (service/find-energiatodistus-digest ts/*db* id)
             :not-in-signing))
    (energiatodistus-service/start-energiatodistus-signing! ts/*db* id)
    (t/is (contains? (service/find-energiatodistus-digest ts/*db* id)
                     :digest))
    (energiatodistus-service/stop-energiatodistus-signing! ts/*db* id)
    (t/is (= (service/find-energiatodistus-digest ts/*db* id)
             :already-signed))))
