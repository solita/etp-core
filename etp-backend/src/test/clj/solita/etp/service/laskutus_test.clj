(ns solita.etp.service.laskutus-test
  (:require [clojure.string :as str]
            [clojure.test :as t]
            [clojure.java.io :as io]
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
            [solita.etp.service.file :as file-service]
            [solita.common.smtp-test :as smtp-test])
  (:import (java.time Instant)))

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

;; All of these tests expect asiakastunnus to match id, which is true in this
;; test data set but not in other environments.

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

(t/deftest safe-subs-test
  (t/is (= nil (laskutus-service/safe-subs nil 1 2)))
  (t/is (= "e" (laskutus-service/safe-subs "hello" 1 2)))
  (t/is (= "ello" (laskutus-service/safe-subs "hello" 1 100)))
  (t/is (= "hello" (laskutus-service/safe-subs "hello" -5 100)))
  (t/is (= "" (laskutus-service/safe-subs "hello" -5 -2)))
  (t/is (= "" (laskutus-service/safe-subs "hello" 1 0))))

(t/deftest find-kuukauden-laskutus-test
  (let [_ (test-data-set)
        laskutus (laskutus-service/find-kuukauden-laskutus ts/*db*)]
    (t/is (= 6 (count laskutus)))))

(t/deftest asiakastiedot-test
  (let [{:keys [yritykset laatijat]} (test-data-set)
        laskutus (laskutus-service/find-kuukauden-laskutus ts/*db*)
        asiakastiedot (laskutus-service/asiakastiedot laskutus)]
    (t/is (= 4 (count asiakastiedot)))
    (t/is (= (set (concat
                   (->> laatijat
                        keys
                        sort
                        (take 2)
                        (apply dissoc laatijat)
                        (map (fn [[id {:keys [etunimi sukunimi henkilotunnus]}]]
                               {:laskutus-asiakastunnus (format "L0%08d" id)
                                :nimi (str etunimi " " sukunimi)
                                :henkilotunnus henkilotunnus})))
                   (map (fn [[id {:keys [nimi henkilotunnus]}]]
                          {:laskutus-asiakastunnus (format "L1%08d" id)
                           :nimi nimi
                           :henkilotunnus (-> laatijat (get id) :henkilotunnus)})
                        yritykset)))
             (->> asiakastiedot
                  (map #(select-keys % [:laskutus-asiakastunnus :nimi :henkilotunnus]))
                  set)))))

(t/deftest asiakastiedot-xml-test
  (let [{:keys [yritykset]} (test-data-set)
        laskutus (laskutus-service/find-kuukauden-laskutus ts/*db*)
        asiakastiedot (laskutus-service/asiakastiedot laskutus)
        yritys-id (-> yritykset keys sort first)
        yritys (get yritykset yritys-id)
        laskutus-asiakastunnus (format "L1%08d" yritys-id)
        asiakastieto (->> asiakastiedot
                          (filter #(= (:laskutus-asiakastunnus %)
                                      (format "L1%08d" yritys-id)))
                          first)
        xml-str (-> asiakastieto
                    laskutus-service/asiakastieto-xml
                    xml/emit-str)]
    (t/is (str/includes? xml-str (str "<AsiakasTunnus>"
                                      laskutus-asiakastunnus
                                      "</AsiakasTunnus")))
    (t/is (str/includes? xml-str (str "<YritysTunnus>"
                                      (:ytunnus yritys)
                                      "</YritysTunnus")))
    (t/is (str/includes? xml-str "<KumppaniNro>ETP</KumppaniNro>"))))

(t/deftest energiatodistus-tilausrivi-text-test
  (let [instant (Instant/parse "2021-01-01T10:10:10.Z")]
    (t/is (= "Energicertifikat 100, datum: 01.01.2021"
             (laskutus-service/energiatodistus-tilausrivi-text 100 instant nil 1)))
    (t/is (= "Energicertifikat 10, datum: 01.01.2021, referens hello"
             (laskutus-service/energiatodistus-tilausrivi-text 10 instant "hello" 1)))
    (t/is (= "Energy performance certificate 1, date: 01.01.2021"
             (laskutus-service/energiatodistus-tilausrivi-text 1 instant nil 2)))
    (t/is (= "Energy performance certificate 123, date: 01.01.2021, reference ref123"
             (laskutus-service/energiatodistus-tilausrivi-text 123 instant "ref123" 2)))
    (t/is (= "Energiatodistus 99, pvm: 01.01.2021"
             (laskutus-service/energiatodistus-tilausrivi-text 99 instant nil 0)))
    (t/is (= "Energiatodistus 99, pvm: 01.01.2021, viite 1234"
             (laskutus-service/energiatodistus-tilausrivi-text 99 instant "1234" 0)))))

(t/deftest laskutustiedot-test
  (let [{:keys [yritykset laatijat energiatodistukset]} (test-data-set)
        laskutus (laskutus-service/find-kuukauden-laskutus ts/*db*)
        laskutustiedot (laskutus-service/laskutustiedot laskutus)
        yritys-id (-> yritykset keys sort first)
        yritys (get yritykset yritys-id)
        yritys-laatija-id (-> laatijat keys sort first)
        yritys-laatija (get laatijat yritys-laatija-id)

        yritys-laskutus-asiakastunnus (format "L1%08d" yritys-id)
        yritys-laskutustieto (->> laskutustiedot
                                  (filter #(= (:laskutus-asiakastunnus %)
                                              yritys-laskutus-asiakastunnus))
                                  first)
        yritys-laatija-energiatodistukset (get-in yritys-laskutustieto
                                                  [:laatijat
                                                   yritys-laatija-id
                                                   :energiatodistukset])
        laatija-id (-> laatijat keys sort last)
        laatija (get laatijat laatija-id)
        laatija-laskutus-asiakastunnus (format "L0%08d" laatija-id)
        laatija-laskutustieto (->> laskutustiedot
                                   (filter #(= (:laskutus-asiakastunnus %)
                                               laatija-laskutus-asiakastunnus))
                                   first)
        laatija-energiatodistukset (get-in laatija-laskutustieto
                                           [:laatijat
                                            laatija-id
                                            :energiatodistukset])
        energiatodistus-ids (-> energiatodistukset keys sort)]
    (t/is (= 4 (count laskutustiedot)))
    (t/is (= {:laskutus-asiakastunnus yritys-laskutus-asiakastunnus
              :laatijat {yritys-laatija-id
                         {:nimi (str (:etunimi yritys-laatija)
                                     " "
                                     (:sukunimi yritys-laatija))}}}
             (-> yritys-laskutustieto
                 (dissoc :laskutuskieli)
                 (xmap/dissoc-in [:laatijat
                                  yritys-laatija-id
                                  :energiatodistukset]))))
    (t/is (= #{(first energiatodistus-ids) (nth energiatodistus-ids 4)}
             (set (map :id yritys-laatija-energiatodistukset))))
    (t/is (= {:laskutus-asiakastunnus laatija-laskutus-asiakastunnus
              :laatijat {laatija-id {:nimi (str (:etunimi laatija)
                                                " "
                                                (:sukunimi laatija))}}}
             (-> laatija-laskutustieto
                 (dissoc :laskutuskieli)
                 (xmap/dissoc-in [:laatijat
                                  laatija-id
                                  :energiatodistukset]))))
    (t/is (= #{(nth energiatodistus-ids 3)}
             (set (map :id laatija-energiatodistukset))))))

(defn tilausrivi-pattern [id allekirjoitusaika]
  (re-pattern (str "<TilausriviTekstiTyyppi><Teksti>"
                   "(Energiatodistus|Energicertifikat|Energy performance certificate) "
                   id
                   ".*"
                   allekirjoitusaika)))

(t/deftest laskutustiedot-xml-test
  (let [{:keys [yritykset]} (test-data-set)
        laskutus (laskutus-service/find-kuukauden-laskutus ts/*db*)
        laskutustiedot (laskutus-service/laskutustiedot laskutus)
        yritys-id (-> yritykset keys sort first)
        yritys (get yritykset yritys-id)
        laskutus-asiakastunnus (format "L1%08d" yritys-id)
        laskutustieto (->> laskutustiedot
                           (filter #(= (:laskutus-asiakastunnus %)
                                       (format "L1%08d" yritys-id)))
                           first)
        laskutustieto-energiatodistukset (-> laskutustieto
                                             :laatijat
                                             vals
                                             first
                                             :energiatodistukset)
        xml-str (->> laskutustieto
                     (laskutus-service/laskutustieto-xml (Instant/now))
                     xml/emit-str)]
    (t/is (str/includes? xml-str (str "<AsiakasNro>"
                                      laskutus-asiakastunnus
                                      "</AsiakasNro")))
    (t/is (str/includes? xml-str "<TilausMaaraArvo>2</TilausMaaraArvo>"))
    (t/is (re-find (tilausrivi-pattern (-> laskutustieto-energiatodistukset
                                           first
                                           :id)
                                       (->> laskutustieto-energiatodistukset
                                            first
                                            :allekirjoitusaika
                                            (.format laskutus-service/date-formatter-fi)))
                   xml-str))
    (t/is (re-find (tilausrivi-pattern (-> laskutustieto-energiatodistukset
                                           second
                                           :id)
                                       (->> laskutustieto-energiatodistukset
                                            second
                                            :allekirjoitusaika
                                            (.format laskutus-service/date-formatter-fi)))
                   xml-str))
    (t/is (str/includes? xml-str "<KumppaniNro>ETP</KumppaniNro>"))))

(t/deftest tasmaytysraportti-test
  (let [{:keys [yritykset laatijat]} (test-data-set)
        yritys-id (-> yritykset keys sort first)
        yritys (get yritykset yritys-id)
        yritys-laskutus-asiakastunnus (format "L1%08d" yritys-id)
        laatija-id (-> laatijat keys sort last)
        laatija (get laatijat laatija-id)
        laatija-laskutus-asiakastunnus (format "L0%08d" laatija-id)
        laskutus (laskutus-service/find-kuukauden-laskutus ts/*db*)
        tasmaytysraportti (laskutus-service/tasmaytysraportti laskutus (Instant/now))]
    (t/is (= 16 (count tasmaytysraportti)))
    (t/is (= "ETP" (ffirst tasmaytysraportti)))
    (t/is (= "ARA" (-> tasmaytysraportti second first)))
    (t/is (= [["Asiakkaiden lukumäärä yhteensä" nil {:v 4 :align :left}]
              ["Myyntitilausten lukumäärä yhteensä" nil {:v 4 :align :left}]
              ["Velotusmyyntitilausten lukumäärä yhteensä" nil {:v 4 :align :left}]
              ["Energiatodistusten lukumäärä yhteensä" nil {:v 6 :align :left}]
              ["Hyvitystilausten lukumäärä yhteensä" nil {:v 0 :align :left}]
              ["Siirrettyjen liitetiedostojen lukumäärä" nil {:v 0 :align :left}]]
             (->> tasmaytysraportti
                  (drop 3)
                  (take 6))))
    (t/is (= [{:v "Tilauslaji" :align :center}
              {:v "Asiakkaan numero" :align :center}
              {:v "Asiakkaan nimi" :align :center}
              {:v "Laskutettava nimike" :align :center}
              {:v "KPL" :align :center}]
             (nth tasmaytysraportti 11)))
    (t/is (->> tasmaytysraportti (drop 12) (map #(-> % first :v)) (every? #(= % "Z001"))))
    (t/is (->> tasmaytysraportti (drop 12) (map #(-> % (nth 3) :v)) (every? #(= % "RA0001"))))

    (t/is (= laatija-laskutus-asiakastunnus
             (-> tasmaytysraportti (nth 13) second :v)))
    (t/is (= (str (:etunimi laatija) " " (:sukunimi laatija))
             (-> tasmaytysraportti (nth 13) (nth 2) :v)))
    (t/is (= 1 (-> tasmaytysraportti (nth 13) last :v)))

    (t/is (= yritys-laskutus-asiakastunnus (-> tasmaytysraportti (nth 14) second :v)))
    (t/is (= (:nimi yritys) (-> tasmaytysraportti (nth 14) (nth 2) :v)))
    (t/is (= 2 (-> tasmaytysraportti (nth 15) last :v)))))

(t/deftest write-tasmaytysraportti-file!-test
  (test-data-set)
  (let [now (Instant/now)
        laskutus (laskutus-service/find-kuukauden-laskutus ts/*db*)
        tasmaytysraportti-file (-> laskutus
                                   (laskutus-service/tasmaytysraportti now)
                                   (laskutus-service/write-tasmaytysraportti-file! now))]
    (t/is (-> tasmaytysraportti-file .exists true?))
    (io/delete-file tasmaytysraportti-file)))

(t/deftest xml-filename-test
  (t/is (= "some-prefix20210114040101123.xml"
           (laskutus-service/xml-filename (Instant/parse "2021-01-14T02:01:01.00Z")
                                          "some-prefix"
                                          123))))

(t/deftest file-key-prefix-test
  (t/is (= "laskutus/2021/20211101121530/"
           (laskutus-service/file-key-prefix (Instant/parse "2021-11-01T10:15:30.00Z")))))

(t/deftest ^:eftest/synchronized do-kuukauden-laskutus-test
  (test-data-set)
  (smtp-test/empty-email-directory!)
  (let [{:keys [started-at]} (laskutus-service/do-kuukauden-laskutus
                              ts/*db*
                              ts/*aws-s3-client*)
        file-key-prefix (laskutus-service/file-key-prefix started-at)]
    (with-open [sftp-connection (sftp/connect! config/laskutus-sftp-host
                                               config/laskutus-sftp-port
                                               config/laskutus-sftp-username
                                               config/laskutus-sftp-password
                                               config/known-hosts-path)]
      (try
        (let [asiakastieto-filenames (sftp/files-in-dir
                                      sftp-connection
                                      laskutus-service/asiakastieto-destination-dir)
              laskutustieto-filenames (sftp/files-in-dir
                                       sftp-connection
                                       laskutus-service/laskutustieto-destination-dir)
              tasmaytysraportti-filename (str "tasmaytysraportti-"
                                              (.format laskutus-service/time-formatter-file
                                                       started-at)
                                              ".pdf")
              tasmaytysraportti-email (-> (smtp-test/email-directory-files)
                                          first
                                          slurp)]
          (t/is (= 4 (count asiakastieto-filenames)))
          (t/is (= 4 (count laskutustieto-filenames)))
          (t/is (every? #(re-matches #"asiakastieto_etp_ara_.+\.xml" %)
                        asiakastieto-filenames))
          (t/is (every? #(re-matches #"laskutustieto_etp_ara_.+\.xml" %)
                        laskutustieto-filenames))
          (t/is (every? #(file-service/find-file ts/*aws-s3-client* %)
                        (->> (concat laskutustieto-filenames)
                             (map #(str file-key-prefix %)))))
          (t/is (every? #(file-service/find-file ts/*aws-s3-client* %)
                        (->> (concat asiakastieto-filenames)
                             (map #(str file-key-prefix %)))))
          (t/is (file-service/find-file
                 ts/*aws-s3-client*
                 (str file-key-prefix tasmaytysraportti-filename)))
          (t/is (str/includes? tasmaytysraportti-email
                               (str "filename=" tasmaytysraportti-filename))))
        (finally (sftp/delete! sftp-connection
                               (str laskutus-service/asiakastieto-destination-dir "*"))
                 (sftp/delete! sftp-connection
                               (str laskutus-service/laskutustieto-destination-dir "*")))))))
