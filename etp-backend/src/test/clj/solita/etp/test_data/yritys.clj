(ns solita.etp.test-data.yritys
  (:require [solita.etp.test-system :as ts]
            [solita.etp.test-data.generators :as generators]
            [solita.etp.schema.yritys :as yritys-schema]
            [solita.etp.service.yritys :as yritys-service]))

(defn generate [n]
  (take n (map #(generators/complete {:ytunnus %
                                      :verkkolaskuoperaattori (rand-int 32)}
                                     yritys-schema/YritysSave)
               generators/unique-ytunnukset)))

(defn insert! [whoami yritykset]
  (mapv #(yritys-service/add-yritys! (ts/db-user (:id whoami))
                                     whoami
                                     %)
        yritykset))

(defn generate-and-insert! [whoami n]
  (insert! (generate n) whoami))
