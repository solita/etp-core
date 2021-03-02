(ns solita.etp.test-data.yritys
  (:require [solita.etp.test-system :as ts]
            [solita.etp.test-data.generators :as generators]
            [solita.etp.schema.yritys :as yritys-schema]
            [solita.etp.service.yritys :as yritys-service]))

(defn generate-adds [n]
  (take n (map #(generators/complete {:ytunnus %
                                      :verkkolaskuoperaattori (rand-int 32)}
                                     yritys-schema/YritysSave)
               generators/unique-ytunnukset)))

(def generate-updates generate-adds)

(defn insert! [yritys-adds laatija-id]
  (mapv #(yritys-service/add-yritys! (ts/db-user laatija-id) {:id laatija-id} %)
        yritys-adds))

(defn generate-and-insert! [n laatija-id]
  (let [yritys-adds (generate-adds n)]
    (zipmap (insert! yritys-adds laatija-id) yritys-adds)))
