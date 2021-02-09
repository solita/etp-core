(ns solita.etp.service.laskutus
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [solita.common.xml :as xml]
            [solita.common.xlsx :as xlsx]
            [solita.common.libreoffice :as libreoffice]
            [solita.common.sftp :as sftp]
            [solita.etp.config :as config]
            [solita.etp.db :as db]
            [solita.etp.service.file :as file-service])
  (:import (java.time Instant LocalDate ZoneId)
           (java.time.temporal ChronoUnit)
           (java.time.format DateTimeFormatter)))

;; *** Require sql functions ***
(db/require-queries 'laskutus)

(def tmp-dir "tmp-laskutus")

(def asiakastieto-dir-path "etp/from_etp/asiakastieto/ara/")
(def asiakastieto-filename-prefix "asiakastieto_etp_ara_")
(def laskutustieto-dir-path "etp/from_etp/laskutustieto/ara/")
(def laskutustieto-filename-prefix "laskutustieto_etp_ara_")

(def asiakastieto-ns "http://kiekuhanke.fi/kieku/asiakasin")
(def laskutustieto-ns "http://www.kiekuhanke.fi/kieku/myyntitilaus")

(def timezone (ZoneId/of "Europe/Helsinki"))
(def date-formatter-fi (.withZone (DateTimeFormatter/ofPattern "dd.MM.yyyy")
                                  timezone))
(def time-formatter-fi (.withZone (DateTimeFormatter/ofPattern "dd.MM.yyyy HH:mm:ss")
                                  timezone))
(def time-formatter-file (.withZone (DateTimeFormatter/ofPattern "yyyyMMddHHmmss")
                                    timezone))

(defn safe-subs [s start end]
  (when s
    (let [start (max start 0)]
      (subs s start (max start (min end (count s)))))))

(defn find-kuukauden-laskutus [db]
  (laskutus-db/select-kuukauden-laskutus db))

(def fields-for-asiakastieto
  #{:laskutus-asiakastunnus :laatija-id :nimi :laskutuskieli :yritys-id :ytunnus
    :valittajatunnus :verkkolaskuosoite :jakeluosoite
    :vastaanottajan-tarkenne :postinumero :postitoimipaikka :maa})

(defn asiakastiedot [laskutus]
  (->> laskutus
       (reduce (fn [acc {:keys [laskutus-asiakastunnus] :as laskutus-item}]
                 (if (contains? acc laskutus-asiakastunnus)
                   acc
                   (assoc acc
                          laskutus-asiakastunnus
                          (select-keys laskutus-item fields-for-asiakastieto))))
               {})
       vals))

(defn asiakastieto-xml
  [{:keys [laskutus-asiakastunnus nimi henkilotunnus laskutuskieli ytunnus
           valittajatunnus verkkolaskuosoite jakeluosoite
           vastaanottajan-tarkenne postinumero postitoimipaikka maa yritys-id]}]
  (let [verkkolaskutus? (and valittajatunnus verkkolaskuosoite)
        long-jakeluosoite? (> (count jakeluosoite) 35)]
    (->> [["AsiakasTunnus" laskutus-asiakastunnus]
          ["TiliryhmaAsiakasKoodi" "Z700"]
          ["AsiakasluokitusKoodi" (if yritys-id "03" "02")]
          ["MaaKoodi" maa]
          ["Nimi1Nimi" (safe-subs nimi 0 35)]
          ["Nimi2Nimi" (safe-subs vastaanottajan-tarkenne 0 35)]
          ["LajittelutietoTeksti" (safe-subs nimi 0 10)]
          ["KieliavainKoodi" (case laskutuskieli
                               1 "V"
                               2 "E"
                               "U")]
          (when ytunnus
            ["YritysTunnus" ytunnus])
          (when ytunnus
            ["AlvNro" (str "FI" (str/replace ytunnus #"-" ""))])
          (when verkkolaskutus?
            ["VastaanottajaOVTTunnus" verkkolaskuosoite])
          (when verkkolaskutus?
            ["VastaanottajaOperaattoriTunnus" valittajatunnus])
          ["LuonnollinenHloKytkin" (if yritys-id "false" "true")]
          ["LaskuKasittelyohjeTeksti" (if verkkolaskutus?
                                        "20000000"
                                        "01000000")]
          ;; TODO YritysmuotoKoodi if yritys is toiminimi.
          ["AsiakasYritystasoinenTietoTyyppi"
           ["YritysTunnus" "7010"]
           ["LajittelutAvainKoodi" "001"]
           ["MaksuehtoavainKoodi" "ZM21"]
           ["KoronlaskentaKoodi" "Z1"]
           ["KassaSuunnitteluryhmaKoodi" "E1"]
           ["MaksukayttaytyminenTallennusKytkin" "true"]]
          ["AsiakasYhteysTietoTyyppi"
           ["LahiOsoite2" (if long-jakeluosoite?
                            (subs jakeluosoite 0 36)
                            "")]
           ["LahiOsoite" (if long-jakeluosoite?
                           (subs jakeluosoite 36)
                           jakeluosoite)]
           ["PaikkakuntaKoodi" postitoimipaikka]
           ["PostiNro" postinumero]
           ["MaaKoodi" maa]
           (when yritys-id
             ["AsiakasHenkiloTunnus" henkilotunnus])]
          ["AsiakasMyyntiJakelutietoTyyppi"
           ["MyyntiOrganisaatioKoodi" "7010"]
           ["JakelutieKoodi" "01"]
           ["SektoriKoodi" "01"]
           ["ValuuttaKoodi" "EUR"]
           ["AsiakasTiliointiryhmaKoodi" (if yritys-id "03" "02")]
           ["MaksuehtoavainKoodi" "ZM21"]]
          ["AsiakasSanomaPerustietoTyyppi"
           ["YleinenLahettajaInformaatioTyyppi"
            ["PorttiNro" (if (= config/environment-alias "prod")
                           "SAPVMP"
                           "SAPVMA")]
            ["KumppaniNro" "ETP"]
            ["KumppanilajiKoodi" "LS"]]]]
         xml/simple-elements
         (apply (fn [& elements]
                  (xml/element (xml/qname asiakastieto-ns "Asiakas")
                               {:xmlns/ns2 asiakastieto-ns}
                               elements))))))

(defn laskutustiedot [laskutus]
  (->> laskutus
       (reduce (fn [acc {:keys [laskutus-asiakastunnus laatija-id laatija-nimi
                               energiatodistus-id allekirjoitusaika
                               laskutuskieli]}]
                 (-> acc
                     (assoc-in [laskutus-asiakastunnus :laskutus-asiakastunnus]
                               laskutus-asiakastunnus)
                     (assoc-in [laskutus-asiakastunnus :laskutuskieli]
                               laskutuskieli)
                     (assoc-in [laskutus-asiakastunnus
                                :laatijat
                                laatija-id
                                :nimi]
                               laatija-nimi)
                     (update-in [laskutus-asiakastunnus
                                 :laatijat
                                 laatija-id
                                 :energiatodistukset]
                                conj
                                {:id energiatodistus-id
                                 :allekirjoitusaika allekirjoitusaika})))
               {})
       vals))

(defn tilausrivi [teksti kieli]
  ["TilausriviTekstiTyyppi"
   ["Teksti" teksti]
   ["TekstiSijaintiKoodi" "0002"]
   ["KieliKoodi" (case kieli
                   1 "SV"
                   2 "EN"
                   "FI")]])

;; TODO translate text
(defn energiatodistus-tilausrivi-text [id allekirjoitusaika laskutuskieli]
  (str "Energiatodistus numero: "
       id
       ", pvm: "
       (.format date-formatter-fi allekirjoitusaika)))

(defn tilausrivit-for-laatija [{:keys [nimi energiatodistukset]} laskutuskieli]
  (cons (tilausrivi nimi laskutuskieli)
        (map (fn [{:keys [id allekirjoitusaika]}]
               (tilausrivi (energiatodistus-tilausrivi-text id
                                                            allekirjoitusaika
                                                            laskutuskieli)
                           laskutuskieli))
             energiatodistukset)))

(defn laskutustieto-xml [now {:keys [laskutus-asiakastunnus laskutuskieli
                                     laatijat]}]
  (let [last-month (.minusMonths now 1)
        last-day-of-last-month (.withDayOfMonth last-month
                                                (.lengthOfMonth last-month))
        formatted-last-day-of-last-month (.format date-formatter-fi
                                                  last-day-of-last-month)]
    (->> [["MyyntiOrganisaatioKoodi" "7010"]
          ["JakelutieKoodi" "13"]
          ["SektoriKoodi" "01"]
          ["TilausLajiKoodi" "Z001"]
          ["LaskuPvm" (.format date-formatter-fi now)]
          ["PalveluLuontiPvm" formatted-last-day-of-last-month]
          ["HinnoitteluPvm" formatted-last-day-of-last-month]
          ["TilausAsiakasTyyppi"
           ["AsiakasNro" laskutus-asiakastunnus]]
          ["LaskuttajaAsiakasTyyppi"
           ["AsiakasNro" "701013A000"]]
          ["MyyntitilausSanomaPerustietoTyyppi"
           ["YleinenLahettajaInformaatioTyyppi"
            ["PorttiNro" (if (= config/environment-alias "prod")
                           "SAPVMP"
                           "SAPVMA")]
            ["KumppaniNro" "ETP"]
            ["KumppanilajiKoodi" "LS"]]]
          (vec (concat ["MyyntiTilausriviTyyppi"
                        ["RiviNro" "10"]
                        ["TilausMaaraArvo" (->> laatijat
                                                vals
                                                (mapcat :energiatodistukset)
                                                count)]
                        ["NimikeNro" "RA0001"]]
                       (mapcat #(tilausrivit-for-laatija % laskutuskieli) (vals laatijat))))]
         xml/simple-elements
         (apply (fn [& elements]
                  (xml/element (xml/qname laskutustieto-ns "Myyntitilaus")
                               {:xmlns/ns2 laskutustieto-ns}
                               elements))))))

(defn tasmaytysraportti-rows [laskutus]
  (let [now (Instant/now)
        row-data (reduce (fn [acc {:keys [laskutus-asiakastunnus nimi]}]
                           (if (get acc laskutus-asiakastunnus)
                             (update-in acc [laskutus-asiakastunnus :count] inc)
                             (assoc acc laskutus-asiakastunnus {:count 1
                                                                :nimi nimi})))
                         {}
                         laskutus)
        laskutus-count (count laskutus)]
    (concat [["ETP" nil nil nil nil (.format time-formatter-fi now)]
             ["ARA" nil (str "Myyntilaskut " (.format date-formatter-fi
                                                      (.minus now 1 ChronoUnit/DAYS)))]
             []
             ["Asiakkaiden lukumäärä yhteensä" nil {:v (count row-data) :align :left}]
             ["Myyntitilausten lukumäärä yhteensä" nil {:v laskutus-count :align :left}]
             ["Velotusmyyntitilausten lukumäärä yhteensä" nil {:v laskutus-count :align :left}]
             ["Hyvitystilausten lukumäärä yhteensä" nil {:v 0 :align :left}]
             ["Siirrettyjen liitetiedostojen lukumäärä" nil {:v 0 :align :left}]
             []
             []
             (mapv #(hash-map :v % :align :center)
                   ["Tilauslaji" "Asiakkaan numero" "Asiakkaan nimi" "Laskutettava nimike" "KPL"])]
            (->> row-data
                 keys
                 sort
                 (map (fn [laskutus-asiakastunnus]
                        [{:v "Z001" :align :center}
                         {:v laskutus-asiakastunnus :align :center}
                         {:v (get-in row-data [laskutus-asiakastunnus :nimi])
                          :align :center}
                         {:v "RA0001" :align :center}
                         {:v (get-in row-data [laskutus-asiakastunnus :count])
                          :align :center}]))))))

(defn create-tasmaytysraportti-file! [tasmaytysraportti]
  (io/make-parents (str tmp-dir "/example.txt"))
  (let [now (Instant/now)
        xlsx-path (str tmp-dir
                       "/tasmaytysraportti-"
                       (.format time-formatter-file now)
                       ".xlsx")
        xlsx (xlsx/create-xlsx)
        sheet (xlsx/create-sheet xlsx "Sheet 0")
        _ (xlsx/set-sheet-landscape sheet true)
        _ (xlsx/fill-sheet! xlsx sheet tasmaytysraportti [5000 5000 7000 5000 5000])
        _ (xlsx/save-xlsx xlsx xlsx-path)
        xlsx-file (io/file xlsx-path)
        xlsx-filename (.getName xlsx-file)
        dir (.getParent xlsx-file)
        pdf-path (str/replace xlsx-path #".xlsx$" ".pdf")
        {:keys [exit err] :as sh-result} (libreoffice/run-with-args
                                          "--convert-to"
                                          "pdf"
                                          xlsx-filename
                                          :dir
                                          dir)
        pdf-exists? (.exists (io/as-file pdf-path))]
    (io/delete-file xlsx-path)
    (if (and (zero? exit) (str/blank? err) pdf-exists?)
      pdf-path
      (throw (ex-info "Converting täsmätytysraportti to PDF failed."
                      (assoc sh-result
                             :type :xlsx-pdf-conversion-failure
                             :xlsx xlsx-filename
                             :pdf-result? pdf-exists?))))))

(defn xml-filename [now filename-prefix idx]
  (str filename-prefix
       (.format time-formatter-file now)
       (format "%03d" idx)
       ".xml"))

(defn xml-file-key [filename]
  (let [numbers (->> filename (re-find #"(\d+)\.xml$") second)]
    (str (subs numbers 0 4) "/" (subs numbers 4 6) "/" filename)))

(defn do-laskutus-file-operations [aws-s3-client sftp-connection xmls
                                   filename-prefix dir-path]
  (sftp/make-directory! sftp-connection dir-path)
  (doseq [[idx xml] (map-indexed vector xmls)
          :let [now (Instant/now)
                filename (xml-filename now filename-prefix idx)
                path (str tmp-dir "/" filename)]]
    (with-open [file (io/writer path)]
      (xml/emit xml file))
    (sftp/upload! sftp-connection path (str dir-path filename))
    (file-service/upsert-file-from-file aws-s3-client
                                        (xml-file-key filename)
                                        (io/file path))
    (io/delete-file path)))

(defn do-kuukauden-laskutus [db aws-s3-client]
  (if (every? #(-> % str/blank? not) [config/laskutus-sftp-host
                                      config/laskutus-sftp-username])
    (do
      (log/info "Starting kuukauden laskutusajo")
      (let [now (LocalDate/now)
            laskutus (find-kuukauden-laskutus db)
            asiakastieto-xmls (->> laskutus
                                   asiakastiedot
                                   (map asiakastieto-xml))
            laskutustieto-xmls (->> laskutus
                                    laskutustiedot
                                    (map #(laskutustieto-xml now %)))]
        (io/make-parents (str tmp-dir "/example.txt"))
        (with-open [sftp-connection (sftp/connect! config/laskutus-sftp-host
                                                   config/laskutus-sftp-port
                                                   config/laskutus-sftp-username
                                                   config/laskutus-sftp-password
                                                   config/known-hosts-path)]
          (do-laskutus-file-operations aws-s3-client
                                       sftp-connection
                                       asiakastieto-xmls
                                       asiakastieto-filename-prefix
                                       asiakastieto-dir-path)
          (do-laskutus-file-operations aws-s3-client
                                       sftp-connection
                                       laskutustieto-xmls
                                       laskutustieto-filename-prefix
                                       laskutustieto-dir-path))
        (log/info "Kuukauden laskutusajo finished")
        nil))
    (log/warn "Sftp parameters not set. Kuukauden laskutus interrupted")))
