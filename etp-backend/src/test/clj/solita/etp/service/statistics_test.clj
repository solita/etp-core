(ns solita.etp.service.statistics-test
  (:require [clojure.test :as t]
            [solita.etp.test-system :as ts]
            [solita.etp.service.statistics :as service]))

(t/use-fixtures :each ts/fixture)
