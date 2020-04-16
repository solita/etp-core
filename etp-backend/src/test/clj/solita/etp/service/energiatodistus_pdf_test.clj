(ns solita.etp.service.energiatodistus-pdf-test
  (:require [clojure.java.io :as io]
            [clojure.test :as t]
            [schema-generators.generators :as g]
            [solita.common.xlsx :as xlsx]
            [solita.etp.schema.energiatodistus :as schema]
            [solita.etp.service.energiatodistus-pdf :as service]
            [solita.etp.service.energiatodistus-test :as energiatodistus-test]))

(def energiatodistus (g/generate
                      schema/EnergiatodistusSave2018
                      energiatodistus-test/energiatodistus-generators))

(t/deftest fill-xlsx-template-test
  (let [file-path (service/fill-xlsx-template energiatodistus)
        _ (println file-path)
        loaded-xlsx (xlsx/load-xlsx file-path)
        sheet-0 (xlsx/get-sheet loaded-xlsx 0)]
    (t/is (-> file-path io/as-file .exists true?))
    (t/is (= (-> energiatodistus :perustiedot :nimi)
             (xlsx/get-cell-value-at sheet-0 "K7")))
    (io/delete-file file-path)))
