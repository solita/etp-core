(ns solita.etp.service.pregenerated-csv
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [solita.etp.service.file :as file-service]
            [solita.etp.service.energiatodistus-csv :as energiatodistus-csv-service]
            [solita.etp.service.kayttaja :as kayttaja-service])
  (:import (java.io File)))

(def pregenerator-whoami {:id (kayttaja-service/system-kayttaja :pregenerator)
                          :rooli -1})

(def packages
  [{:destination "energiatodistukset-banks.csv"
    :columns energiatodistus-csv-service/public-columns ;; TODO Add kiinteistötunnus, postitoimipaikka
    :query {:where nil}}
   {:destination "energiatodistukset-tilastokeskus.csv"
    :columns energiatodistus-csv-service/public-columns ;; TODO TBD
    :query {:where nil}}
   {:destination "energiatodistus-anonymous.csv"
    :columns energiatodistus-csv-service/public-columns ;; TODO TBD
    :query {:where nil}}])

(defn energiatodistukset-csv [db query columns]
  (energiatodistus-csv-service/energiatodistukset-csv db
                                                      pregenerator-whoami
                                                      query
                                                      columns))

(defn write-csv! [energiatodistukset file]
  (with-open [out (io/writer file)]
    (let [write! (fn [buf]
                   (.write out buf))]
      (energiatodistukset write!))))

(defn pregenerate-csv [db aws-s3-client {:keys [destination query columns]}]
  (let [file (File/createTempFile "energiatodistukset-" ".csv")]
    (try
      (let [energiatodistukset (energiatodistukset-csv db query columns)]
        (write-csv! energiatodistukset file))
      (log/info "Wrote energiatodistukset to" (.getPath file))
      (file-service/upsert-file-from-file aws-s3-client
                                          destination
                                          file)
      (log/info "Did upsert energiatodistukset into" destination)
      (finally
        (.delete file)
        (log/info "Deleted" (.getPath file))))))

(defn pregenerate [db aws-s3-client]
  (doseq [package packages]
    (pregenerate-csv db aws-s3-client package)))
