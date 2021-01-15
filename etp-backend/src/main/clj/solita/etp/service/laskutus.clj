(ns solita.etp.service.laskutus
  (:require [clojure.string :as str]
            [solita.common.xml :as xml]
            [solita.etp.db :as db]))

;; *** Require sql functions ***
(db/require-queries 'laskutus)

(def asiakastieto-ns "http://kiekuhanke.fi/kieku/asiakasin")

(defn find-kuukauden-laskutus [db]
  (laskutus-db/select-kuukauden-laskutus db))

(def fields-for-asiakastieto
  #{:asiakastunnus :nimi :laskutuskieli :yritys-id :ytunnus
    :valittajatunnus :verkkolaskuosoite :jakeluosoite
    :vastaanottajan-tarkenne :postinumero :postitoimipaikka :maa})

(defn asiakastiedot [laskutus]
  (reduce (fn [acc {:keys [asiakastunnus] :as laskutus-item}]
            (if (contains? acc asiakastunnus)
              acc
              (assoc acc asiakastunnus (select-keys laskutus-item
                                                    fields-for-asiakastieto))))
          {}
   laskutus))

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
