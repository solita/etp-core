(ns solita.etp.service.laskutus
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [solita.common.xml :as xml]
            [solita.common.sftp :as sftp]
            [solita.etp.config :as config]
            [solita.etp.db :as db]
            [solita.etp.service.file :as file-service])
  (:import (java.time Instant LocalDate ZoneId)
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
(def date-formatter-file (.withZone (DateTimeFormatter/ofPattern "yyyyMMddHHmmss")
                                    timezone))

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

(defn safe-subs [s start end]
  (if (>= (count s) end)
    (subs s start end)
    s))

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
                               energiatodistus-kieli]
                        :as laskutus-item}]
                 (-> acc
                     (assoc-in [laskutus-asiakastunnus :laskutus-asiakastunnus]
                               laskutus-asiakastunnus)
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
                                 :allekirjoitusaika allekirjoitusaika
                                 :kieli energiatodistus-kieli})))
               {})
       vals))

(defn tilausrivi [teksti kieli]
  ["TilausriviTekstiTyyppi"
   ["Teksti" teksti]
   ["TekstiSijaintiKoodi" "0002"]
   ["KieliKoodi" (if (= kieli 1) "sv" "fi")]])

(defn tilausrivit-for-laatija [{:keys [nimi energiatodistukset]}]
  (cons (tilausrivi nimi 0)
        (map #(tilausrivi (str "Energiatodistus numero: "
                               (:id %)
                               ", pvm: "
                               (.format date-formatter-fi
                                        (:allekirjoitusaika %)))
                          (:kieli %))
             energiatodistukset)))

(defn laskutustieto-xml [now {:keys [laskutus-asiakastunnus laatijat]
                              :as laskutustieto}]
  (->> [["MyyntiOrganisaatioKoodi" "7010"]
        ["JakelutieKoodi" "13"]
        ["SektoriKoodi" "01"]
        ["TilausLajiKoodi" "Z001"]
        ["LaskuPvm" (.format date-formatter-fi now)]
        ["PalveluLuontiPvm" "TODO"]
        ["HinnoitteluPvm" "TODO"]
        ["SopimusPvm" "TODO"]
        ["SopimusNro" "TODO"]
        ["TiliointiViiteKoodi" "TODO"]
        ["TyomaaAvainKoodi" "TODO"]
        ["TilausAsiakasTyyppi"
         ["AsiakasNro" laskutus-asiakastunnus]]
        ["LaskuttajaAsiakasTyyppi"
         ["AsiakasNro" "701013A000"]]
        ["MyyntitilausSanomaPerustietoTyyppi"
         ["YleinenLahettajaInformaatioTyyppi"
          ["PorttiNro" "SAPVXA"]
          ["KumppaniNro" "ETP"]
          ["KumppanilajiKoodi" "LS"]]]
        (vec (concat ["MyyntiTilausriviTyyppi"
                      ["RiviNro" "10"]
                      ["TilausMaaraArvo" (->> laatijat
                                              vals
                                              (mapcat :energiatodistukset)
                                              count)]
                      ["NimikeNro" "RA0001"]]
                     (mapcat tilausrivit-for-laatija (vals laatijat))))]
       xml/simple-elements
       (apply (fn [& elements]
                (xml/element (xml/qname laskutustieto-ns "Myyntitilaus")
                             {:xmlns/ns2 laskutustieto-ns}
                             elements)))))

(defn xml-filename [now filename-prefix idx]
  (str filename-prefix
       (.format date-formatter-file now)
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
        (io/delete-file tmp-dir)
        (log/info "Kuukauden laskutusajo finished")
        nil))
    (log/warn "Sftp parameters not set. Kuukauden laskutus interrupted")))
