(ns solita.etp.service.laskutus
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [solita.common.xml :as xml]
            [solita.common.sftp :as sftp]
            [solita.etp.config :as config]
            [solita.etp.db :as db]
            [solita.etp.service.file :as file-service])
  (:import (java.time LocalDate ZoneId)
           (java.time.format DateTimeFormatter)))

;; *** Require sql functions ***
(db/require-queries 'laskutus)

(def tmp-dir "tmp-laskutus")

(def asiakastieto-filename-prefix "asiakastieto_etp_ara_")
(def laskutustieto-filename-prefix "laskutustieto_etp_ara_")

(def asiakastieto-ns "http://kiekuhanke.fi/kieku/asiakasin")
(def laskutustieto-ns "http://www.kiekuhanke.fi/kieku/myyntitilaus")

(def timezone (ZoneId/of "Europe/Helsinki"))
(def date-formatter-fi (.withZone (DateTimeFormatter/ofPattern "dd.MM.yyyy")
                                  timezone))
(def date-formatter-file (.withZone (DateTimeFormatter/ofPattern "yyyyMMdd")
                                    timezone))

(defn find-kuukauden-laskutus [db]
  (laskutus-db/select-kuukauden-laskutus db))

(def fields-for-asiakastieto
  #{:asiakastunnus :nimi :laskutuskieli :yritys-id :ytunnus
    :valittajatunnus :verkkolaskuosoite :jakeluosoite
    :vastaanottajan-tarkenne :postinumero :postitoimipaikka :maa})

(defn asiakastiedot [laskutus]
  (->> laskutus
       (reduce (fn [acc {:keys [asiakastunnus] :as laskutus-item}]
                 (if (contains? acc asiakastunnus)
                   acc
                   (assoc acc asiakastunnus (select-keys laskutus-item
                                                         fields-for-asiakastieto))))
               {})
       vals))

(defn asiakastieto-xml
  [{:keys [asiakastunnus nimi laskutuskieli ytunnus valittajatunnus
           verkkolaskuosoite jakeluosoite vastaanottajan-tarkenne postinumero
           postitoimipaikka maa yritys-id]}]
  (->> [["AsiakasTunnus" asiakastunnus]
        ["AsiakasluokitusKoodi" "03"]
        ["MaaKoodi" "FI"]
        ["Nimi1Nimi" nimi]
        ["Nimi2Nimi" "TODO"]
        ["LajittelutietoTeksti" "TODO"]
        ["KieliavainKoodi" "U"]
        ["YritysTunnus" ytunnus]
        (when ytunnus
          ["AlvNro" (str "FI" (str/replace ytunnus #"-" ""))])
        ["VastaanottajaOVTTunnus" verkkolaskuosoite]
        ["VastaanottajaOperaattoriTunnus" valittajatunnus]
        ["LuonnollinenHloKytkin" (if yritys-id "false" "true")]
        ["LaskuKasittelyohjeTeksti" "01000000"]
        ["AsiakasYritystasoinenTietoTyyppi"
         ["YritysTunnus" "7010"]]
        ["AsiakasYhteysTietoTyyppi"
         ["LahiOsoite2" vastaanottajan-tarkenne]
         ["LahiOsoite" jakeluosoite]
         ["PaikkakuntaKoodi" postitoimipaikka]
         ["PostiNro" postinumero]
         ["MaaKoodi" maa]]
        ["AsiakasMyyntiJakelutietoTyyppi"
         ["MyyntiOrganisaatioKoodi" "7010"]
         ["JakelutieKoodi" "01"]
         ["SektoriKoodi" "01"]
         ["ValuuttaKoodi" "EUR"]
         ["AsiakasTiliointiryhmaKoodi" "03"]
         ["MaksuehtoavainKoodi" "ZM21"]]
        ["AsiakasSanomaPerustietoTyyppi"
         ["YleinenLahettajaInformaatioTyyppi"
          ["PorttiNro" "SAPVXA"]
          ["KumppaniNro" "ETP"]
          ["KumppanilajiKoodi" "LS"]]]]
       xml/simple-elements
       (apply (fn [& elements]
                (xml/element (xml/qname asiakastieto-ns "Asiakas")
                             {:xmlns/ns2 asiakastieto-ns}
                             elements)))))

(defn laskutustiedot [laskutus]
  (->> laskutus
       (reduce (fn [acc {:keys [asiakastunnus laatija-id laatija-nimi
                               energiatodistus-id allekirjoitusaika energiatodistus-kieli]
                        :as laskutus-item}]
                 (-> acc
                     (assoc-in [asiakastunnus :asiakastunnus] asiakastunnus)
                     (assoc-in [asiakastunnus :laatijat laatija-id :nimi] laatija-nimi)
                     (update-in [asiakastunnus :laatijat laatija-id :energiatodistukset]
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


(defn laskutustieto-xml [now {:keys [asiakastunnus laatijat] :as laskutustieto}]
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
         ["AsiakasNro" asiakastunnus]]
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
       "02"
       (format "%07d" idx)
       ".xml"))

(defn xml-file-key [now filename]
  (format "%d/%02d/%s" (.getYear now) (.getMonthValue now) filename))

(defn do-laskutus-file-operations [aws-s3-client sftp-connection now xmls filename-prefix]
  (doseq [[idx xml] (map-indexed vector xmls)
          :let [filename (xml-filename now filename-prefix idx)
                path (str tmp-dir "/" filename)]]
    (with-open [file (io/writer path)]
      (xml/emit xml file))
    ;; TODO directory structure?
    (sftp/make-directory! sftp-connection "etp")
    (sftp/upload! sftp-connection path (str "etp/" filename))
    (file-service/upsert-file-from-file aws-s3-client
                                        (xml-file-key now filename)
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
                                       now
                                       asiakastieto-xmls
                                       asiakastieto-filename-prefix)
          (do-laskutus-file-operations aws-s3-client
                                       sftp-connection
                                       now
                                       laskutustieto-xmls
                                       laskutustieto-filename-prefix))
        (io/delete-file tmp-dir)
        (log/info "Kuukauden laskutusajo finished")
        nil))
    (log/warn "Sftp parameters not set. Kuukauden laskutus interrupted")))
