(ns solita.etp.service.laskutus
  (:require [clojure.string :as str]
            [solita.common.xml :as xml]
            [solita.etp.db :as db])
  (:import (java.time LocalDate ZoneId)
           (java.time.format DateTimeFormatter)))

;; *** Require sql functions ***
(db/require-queries 'laskutus)

(def asiakastieto-ns "http://kiekuhanke.fi/kieku/asiakasin")
(def laskutustieto-ns "http://www.kiekuhanke.fi/kieku/myyntitilaus")

(def timezone (ZoneId/of "Europe/Helsinki"))
(def date-formatter (.withZone (DateTimeFormatter/ofPattern "dd.MM.yyyy") timezone))

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
        ["AlvNro" (str "FI" (str/replace ytunnus #"-" ""))]
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
                             elements)))
   xml/emit-str))

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
                               (.format date-formatter (:allekirjoitusaika %)))
                          (:kieli %))
             energiatodistukset)))


(defn laskutustieto-xml [{:keys [asiakastunnus laatijat] :as laskutustieto}]
  (->> [["MyyntiOrganisaatioKoodi" "7010"]
        ["JakelutieKoodi" "13"]
        ["SektoriKoodi" "01"]
        ["TilausLajiKoodi" "Z001"]
        ["LaskuPvm" (.format date-formatter (LocalDate/now))]
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
                             elements)))
   xml/emit-str))
