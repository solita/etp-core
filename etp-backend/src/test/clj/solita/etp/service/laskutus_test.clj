(ns solita.etp.service.laskutus-test
  (:require [clojure.string :as str]
            [clojure.test :as t]
            [clojure.java.jdbc :as jdbc]
            [solita.common.map :as xmap]
            [solita.common.xml :as xml]
            [solita.common.sftp :as sftp]
            [solita.etp.config :as config]
            [solita.etp.test-system :as ts]
            [solita.etp.test-data.yritys :as yritys-test-data]
            [solita.etp.test-data.laatija :as laatija-test-data]
            [solita.etp.test-data.energiatodistus :as energiatodistus-test-data]
            [solita.etp.service.energiatodistus :as energiatodistus-service]
            [solita.etp.service.laskutus :as laskutus-service]
            [solita.etp.service.file :as file-service])
  (:import (java.time Instant LocalDate)))

(t/use-fixtures :each ts/fixture)

;; Yritys 1 has laatija 1.
;; Yritys 2 has laatija 2.
;; Laatija 3 and 4 have no yritys.
;; Laatija 1 has energiatodistukset 1 and 5, laatija 2 has 2 and 6 etc.
;; Laskut from energiatodistukset 1 and 5 should go to yritys 1.
;; Laskut from energiatodistukset 2 and 6 should go to yritys 2.
;; Laskut energiatodistukset 3, 4, 7 and 8 should go their laatijat.
;; Energiatodistukset 1-7 are signed.
;; Energiatodistukset 1-6 are signed during last month.

(defn test-data-set []
  (let [laatijat (laatija-test-data/generate 4)
        laatija-ids (laatija-test-data/insert! laatijat)
        yritykset (yritys-test-data/generate 2)
        yritys-ids (->> (interleave laatija-ids yritykset)
                        (partition 2)
                        (mapcat #(yritys-test-data/insert!
                                  {:id (first %)}
                                  [(second %)])))
        energiatodistukset (->> (interleave
                                 (energiatodistus-test-data/generate 4 2013 true)
                                 (energiatodistus-test-data/generate 4 2018 true))
                                (interleave (cycle (concat yritys-ids [nil nil])))
                                (partition 2)
                                (map #(assoc (second %)
                                             :laskutettava-yritys-id
                                             (first %))))
        energiatodistus-ids (->> (interleave (cycle laatija-ids)
                                             energiatodistukset)
                                 (partition 2)
                                 (mapcat #(energiatodistus-test-data/insert!
                                           {:id (first %)}
                                           [(second %)]))
                                 (map :id)
                                 doall)]
    (doseq [[laatija-id energiatodistus-id] (->> (interleave (cycle laatija-ids)
                                                             energiatodistus-ids)
                                                 (partition 2)
                                                 (take 7))]
      (energiatodistus-service/start-energiatodistus-signing! ts/*db*
                                                              {:id laatija-id}
                                                              energiatodistus-id)
      (energiatodistus-service/end-energiatodistus-signing! ts/*db*
                                                            {:id laatija-id}
                                                            energiatodistus-id))
    (jdbc/execute! ts/*db* ["UPDATE energiatodistus SET allekirjoitusaika = allekirjoitusaika - interval '1 month' WHERE id <= 6"])
    {:laatijat (apply assoc {} (interleave laatija-ids laatijat))
     :yritykset (apply assoc {} (interleave yritys-ids yritykset))
     :energiatodistukset (apply assoc {} (interleave energiatodistus-ids
                                                     energiatodistukset))}))

(t/deftest find-kuukauden-laskutus-test
  (let [_ (test-data-set)
        laskutus (laskutus-service/find-kuukauden-laskutus ts/*db*)]
    (t/is (= 6 (count laskutus)))))

(t/deftest asiakastiedot-test
  (let [{:keys [yritykset laatijat]} (test-data-set)
        laskutus (laskutus-service/find-kuukauden-laskutus ts/*db*)
        asiakastiedot (laskutus-service/asiakastiedot laskutus)]
    (t/is (= 4 (count asiakastiedot)))
    (t/is (= (set (concat (->> laatijat
                               keys
                               sort
                               (take 2)
                               (apply dissoc laatijat)
                               (map (fn [[id {:keys [etunimi sukunimi]}]]
                                      {:asiakastunnus (format "L0%08d" id)
                                       :nimi (str etunimi " " sukunimi)})))
                          (map (fn [[id {:keys [nimi]}]]
                                 {:asiakastunnus (format "L1%08d" id)
                                  :nimi nimi})
                               yritykset)))
             (->> asiakastiedot
                  (map #(select-keys % [:asiakastunnus :nimi]))
                  set)))))

(t/deftest asiakastiedot-xml-test
  (let [{:keys [yritykset]} (test-data-set)
        laskutus (laskutus-service/find-kuukauden-laskutus ts/*db*)
        asiakastiedot (laskutus-service/asiakastiedot laskutus)
        yritys-id (-> yritykset keys sort first)
        yritys (get yritykset yritys-id)
        asiakastunnus (format "L1%08d" yritys-id)
        asiakastieto (->> asiakastiedot
                          (filter #(= (:asiakastunnus %)
                                      (format "L1%08d" yritys-id)))
                          first)
        xml-str (-> asiakastieto
                    laskutus-service/asiakastieto-xml
                    xml/emit-str)]
    (t/is (str/includes? xml-str (str "<AsiakasTunnus>"
                                      asiakastunnus
                                      "</AsiakasTunnus")))
    (t/is (str/includes? xml-str (str "<YritysTunnus>"
                                      (:ytunnus yritys)
                                      "</YritysTunnus")))
    (t/is (str/includes? xml-str "<KumppaniNro>ETP</KumppaniNro>"))))

(t/deftest laskutustiedot-test
  (let [{:keys [yritykset laatijat energiatodistukset]} (test-data-set)
        laskutus (laskutus-service/find-kuukauden-laskutus ts/*db*)
        laskutustiedot (laskutus-service/laskutustiedot laskutus)
        yritys-id (-> yritykset keys sort first)
        yritys (get yritykset yritys-id)
        yritys-laatija-id (-> laatijat keys sort first)
        yritys-laatija (get laatijat yritys-laatija-id)
        yritys-asiakastunnus (format "L1%08d" yritys-id)
        yritys-laskutustieto (->> laskutustiedot
                                  (filter #(= (:asiakastunnus %)
                                              yritys-asiakastunnus))
                                  first)
        yritys-laatija-energiatodistukset (get-in yritys-laskutustieto
                                                  [:laatijat
                                                   yritys-laatija-id
                                                   :energiatodistukset])
        laatija-id (-> laatijat keys sort last)
        laatija (get laatijat laatija-id)
        laatija-asiakastunnus (format "L0%08d" laatija-id)
        laatija-laskutustieto (->> laskutustiedot
                                   (filter #(= (:asiakastunnus %)
                                               laatija-asiakastunnus))
                                   first)
        laatija-energiatodistukset (get-in laatija-laskutustieto
                                           [:laatijat
                                            laatija-id
                                            :energiatodistukset])
        energiatodistus-ids (-> energiatodistukset keys sort)]
    (t/is (= 4 (count laskutustiedot)))
    (t/is (= {:asiakastunnus yritys-asiakastunnus
              :laatijat {yritys-laatija-id
                         {:nimi (str (:etunimi yritys-laatija)
                                     " "
                                     (:sukunimi yritys-laatija))}}}
             (xmap/dissoc-in yritys-laskutustieto [:laatijat
                                                   yritys-laatija-id
                                                   :energiatodistukset])))
    (t/is (= #{(first energiatodistus-ids) (nth energiatodistus-ids 4)}
             (set (map :id yritys-laatija-energiatodistukset))))

    (t/is (= {:asiakastunnus laatija-asiakastunnus
              :laatijat {laatija-id {:nimi (str (:etunimi laatija)
                                                " "
                                                (:sukunimi laatija))}}}
             (xmap/dissoc-in laatija-laskutustieto [:laatijat
                                                    laatija-id
                                                    :energiatodistukset])))
    (t/is (= #{(nth energiatodistus-ids 3)}
             (set (map :id laatija-energiatodistukset))))))

(t/deftest laskutustiedot-xml-test
  (let [{:keys [yritykset]} (test-data-set)
        laskutus (laskutus-service/find-kuukauden-laskutus ts/*db*)
        laskutustiedot (laskutus-service/laskutustiedot laskutus)
        yritys-id (-> yritykset keys sort first)
        yritys (get yritykset yritys-id)
        asiakastunnus (format "L1%08d" yritys-id)
        laskutustieto (->> laskutustiedot
                           (filter #(= (:asiakastunnus %)
                                       (format "L1%08d" yritys-id)))
                           first)
        laskutustieto-energiatodistukset (-> laskutustieto
                                             :laatijat
                                             vals
                                             first
                                             :energiatodistukset)
        xml-str (->> laskutustieto
                     (laskutus-service/laskutustieto-xml (LocalDate/now))
                     xml/emit-str)]
    (t/is (str/includes? xml-str (str "<AsiakasNro>"
                                      asiakastunnus
                                      "</AsiakasNro")))
    (t/is (str/includes? xml-str "<TilausMaaraArvo>2</TilausMaaraArvo>"))
    (t/is (str/includes? xml-str (str "<TilausriviTekstiTyyppi><Teksti>"
                                      "Energiatodistus numero: "
                                      (-> laskutustieto-energiatodistukset
                                          first
                                          :id)
                                      ", pvm: ")))
    (t/is (str/includes? xml-str (str "<TilausriviTekstiTyyppi><Teksti>"
                                      "Energiatodistus numero: "
                                      (-> laskutustieto-energiatodistukset
                                          second
                                          :id)
                                      ", pvm: ")))
    (t/is (str/includes? xml-str (str "pvm: "
                                      (->> laskutustieto-energiatodistukset
                                           first
                                           :allekirjoitusaika
                                           (.format laskutus-service/date-formatter-fi))
                                      "</Teksti>")))
    (t/is (str/includes? xml-str "<KumppaniNro>ETP</KumppaniNro>"))))

(t/deftest xml-filename-test
  (t/is (= "some-prefix20210114040101123.xml"
           (laskutus-service/xml-filename (Instant/parse "2021-01-14T02:01:01.00Z")
                                          "some-prefix"
                                          123))))

(t/deftest xml-file-key
  (t/is (= "2021/01/some-prefix20210114040101123.xml"
           (laskutus-service/xml-file-key "some-prefix20210114040101123.xml"))))

(t/deftest do-kuukauden-laskutus-test
  (test-data-set)
  (laskutus-service/do-kuukauden-laskutus ts/*db* ts/*aws-s3-client*)
  (try
    (with-open [sftp-connection (sftp/connect! config/laskutus-sftp-host
                                               config/laskutus-sftp-port
                                               config/laskutus-sftp-username
                                               config/laskutus-sftp-password
                                               config/known-hosts-path)]
      (try
        (let [asiakastieto-filenames (sftp/files-in-dir
                                      sftp-connection
                                      laskutus-service/asiakastieto-dir-path)
              laskutustieto-filenames (sftp/files-in-dir
                                       sftp-connection
                                       laskutus-service/laskutustieto-dir-path)]
          (t/is (= 4 (count asiakastieto-filenames)))
          (t/is (= 4 (count laskutustieto-filenames)))
          (t/is (every? #(re-matches #"asiakastieto_etp_ara_.+\.xml" %)
                        asiakastieto-filenames))
          (t/is (every? #(re-matches #"laskutustieto_etp_ara_.+\.xml" %)
                        laskutustieto-filenames))
          (t/is (every? #(file-service/find-file ts/*aws-s3-client* %)
                        (->> (concat laskutustieto-filenames)
                             (map laskutus-service/xml-file-key)))))
        (finally (sftp/delete! sftp-connection (str laskutus-service/asiakastieto-dir-path "*"))
                 (sftp/delete! sftp-connection (str laskutus-service/laskutustieto-dir-path "*")))))))
