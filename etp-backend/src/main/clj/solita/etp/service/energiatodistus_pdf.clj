(ns solita.etp.service.energiatodistus-pdf
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [puumerkki.pdf :as puumerkki]
            [solita.common.xlsx :as xlsx]
            [solita.etp.service.energiatodistus :as energiatodistus-service]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.service.file :as file-service])
  (:import (java.time Instant LocalDate ZoneId)
           (java.time.format DateTimeFormatter)
           (java.util Locale)
           (java.text DecimalFormatSymbols DecimalFormat)
           (java.math RoundingMode)
           (org.apache.pdfbox.multipdf Overlay)
           (org.apache.pdfbox.multipdf Overlay$Position)
           (org.apache.pdfbox.pdmodel PDDocument)
           (java.util HashMap)
           (java.awt Font Color)
           (java.awt.image BufferedImage)
           (javax.imageio ImageIO)))

(def xlsx-template-path "energiatodistus-template.xlsx")
(def watermark-path-fi "watermark-fi.pdf")
(def sheet-count 8)
(def tmp-dir "tmp/")

(def date-formatter (DateTimeFormatter/ofPattern "dd.MM.yyyy"))
(def timezone (ZoneId/of "Europe/Helsinki"))
(def time-formatter (.withZone (DateTimeFormatter/ofPattern "dd.MM.yyyy HH:mm:ss")
                               timezone))

(def locale (Locale. "fi" "FI"))
(def format-symbols (DecimalFormatSymbols. locale))

(defn format-number [x dp percent?]
  (when x
    (let [format (if percent? "#.# %" "#.#")
          number-format (doto (java.text.DecimalFormat. format  format-symbols)
                          (.setMinimumFractionDigits (if dp dp 0))
                          (.setMaximumFractionDigits (if dp dp Integer/MAX_VALUE))
                          (.setRoundingMode RoundingMode/HALF_UP))]
      (.format number-format (bigdec x)))))

(defn sis-kuorma [energiatodistus]
  (->> energiatodistus
       :lahtotiedot
       :sis-kuorma
       (reduce-kv (fn [acc k {:keys [kayttoaste lampokuorma]}]
                    (update acc kayttoaste #(merge % {k lampokuorma})))
                  {})
       (into (sorted-map))
       seq
       (into [])))

(defn mappings []
  (let [now (Instant/now)
        today (LocalDate/now)]
    {0 {"K7" {:path [:perustiedot :nimi]}
        "K8" {:path [:perustiedot :katuosoite-fi]}

        ;; TODO needs luokittelu for postitoimipaikka
        "K9" {:f #(str (-> % :perustiedot :postinumero) " " "Helsinki")}
        "K12" {:path [:perustiedot :rakennustunnus]}
        "K13" {:path [:perustiedot :valmistumisvuosi]}

        "K14" {:path [:perustiedot :alakayttotarkoitus-fi]}
        "K16" {:path [:id]}

        "D19" {:f (fn [energiatodistus]
                    (if (= (-> energiatodistus :perustiedot :laatimisvaihe) 0)
                      "☒ Uudelle rakennukselle rakennuslupaa haettaessa"
                      "☐ Uudelle rakennukselle rakennuslupaa haettaessa"))}
        "D20" {:f (fn [energiatodistus]
                    (if (= (-> energiatodistus :perustiedot :laatimisvaihe) 1)
                      "☒ Uudelle rakennukselle käyttöönottovaiheessa"
                      "☐ Uudelle rakennukselle käyttöönottovaiheessa"))}
        "D21" {:f (fn [energiatodistus]
                    (if (= (-> energiatodistus :perustiedot :laatimisvaihe) 2)
                      "☒ Olemassa olevalle rakennukselle, havainnointikäynnin päivämäärä:"
                      "☐ Olemassa olevalle rakennukselle, havainnointikäynnin päivämäärä:"))}

        "M21" {:f (fn [energiatodistus]
                    (some->> energiatodistus :perustiedot :havainnointikaynti (.format date-formatter)))}

        ;; TODO M37 E-luvun vaatimus
        "M36" {:path [:tulokset :e-luku]}

        "B42" {:path [:laatija-fullname]}
        "J42" {:path [:perustiedot :yritys :nimi]}

        "B50" {:f (fn [_] (.format date-formatter today))}
        "K50" {:f (fn [_] (.format date-formatter (.plusYears today 10)))}}
     1 {"F5" {:f #(-> %
                      :lahtotiedot
                      :lammitetty-nettoala
                      (format-number 1 false)
                      (str " m²"))}
        "F6" {:path [:lahtotiedot :lammitys :kuvaus-fi]}
        "F7" {:path [:lahtotiedot :ilmanvaihto :kuvaus-fi]}
        "F14" {:path [:tulokset :kaytettavat-energiamuodot :kaukolampo] :dp 0}
        "G14" {:path [:tulokset :kaytettavat-energiamuodot :kaukolampo-nettoala] :dp 0}
        "H14" {:path [:tulokset :kaytettavat-energiamuodot :kaukolampo-kerroin]}
        "I14" {:path [:tulokset :kaytettavat-energiamuodot :kaukolampo-nettoala-kertoimella :dp 0]}
        "F15" {:path [:tulokset :kaytettavat-energiamuodot :sahko] :dp 0}
        "G15" {:path [:tulokset :kaytettavat-energiamuodot :sahko-nettoala] :dp 0}
        "H15" {:path [:tulokset :kaytettavat-energiamuodot :sahko-kerroin]}
        "I15" {:path [:tulokset :kaytettavat-energiamuodot :sahko-nettoala-kertoimella] :dp 0}
        "F16" {:path [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine] :dp 0}
        "G16" {:path [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-nettoala] :dp 0}
        "H16" {:path [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-kerroin]}
        "I16" {:path [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-nettoala-kertoimella] :dp 0}
        "F17" {:path [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine] :dp 0}
        "G17" {:path [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-nettoala] :dp 0}
        "H17" {:path [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-kerroin]}
        "I17" {:path [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-nettoala-kertoimella] :dp 0}
        "F18" {:path [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys] :dp 0}
        "G18" {:path [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-nettoala] :dp 0}
        "H18" {:path [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-kerroin]}
        "I18" {:path [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-nettoala-kertoimella] :dp 0}

        "I20" {:path [:tulokset :e-luku]}

        ;; TODO Käytetty E-luvun luokittelu asteikko

        ;; TODO Luokkien rajat asteikolla

        ;; TODO Tämän rakennuksen energiatehokkuusluokka

        "C38" {:path [:perustiedot :keskeiset-suositukset-fi]}}
     2 {"D4" {:path [:perustiedot :alakayttotarkoitus-fi]}
        "D5" {:path [:perustiedot :valmistumisvuosi]}
        "F5" {:path [:lahtotiedot :lammitetty-nettoala] :dp 1}
        "D7" {:path [:lahtotiedot :rakennusvaippa :ilmanvuotoluku] :dp 1}

        "D10" {:path [:lahtotiedot :rakennusvaippa :ulkoseinat :ala] :dp 1}
        "E10" {:path [:lahtotiedot :rakennusvaippa :ulkoseinat :U] :dp 2}
        "F10" {:path [:lahtotiedot :rakennusvaippa :ulkoseinat :UA] :dp 1}
        "G10" {:path [:lahtotiedot :rakennusvaippa :ulkoseinat :osuus-lampohaviosta] :dp 0 :percent? true}
        "D11" {:path [:lahtotiedot :rakennusvaippa :ylapohja :ala] :dp 1}
        "E11" {:path [:lahtotiedot :rakennusvaippa :ylapohja :U] :dp 2}
        "F11" {:path [:lahtotiedot :rakennusvaippa :ylapohja :UA] :dp 1}
        "G11" {:path [:lahtotiedot :rakennusvaippa :ylapohja :osuus-lampohaviosta] :dp 0 :percent? true}
        "D12" {:path [:lahtotiedot :rakennusvaippa :alapohja :ala] :dp 1}
        "E12" {:path [:lahtotiedot :rakennusvaippa :alapohja :U] :dp 2}
        "F12" {:path [:lahtotiedot :rakennusvaippa :alapohja :UA] :dp 1}
        "G12" {:path [:lahtotiedot :rakennusvaippa :alapohja :osuus-lampohaviosta] :dp 0 :percent? true}
        "D13" {:path [:lahtotiedot :rakennusvaippa :ikkunat :ala] :dp 1}
        "E13" {:path [:lahtotiedot :rakennusvaippa :ikkunat :U] :dp 2}
        "F13" {:path [:lahtotiedot :rakennusvaippa :ikkunat :UA] :dp 1}
        "G13" {:path [:lahtotiedot :rakennusvaippa :ikkunat :osuus-lampohaviosta] :dp 0 :percent? true}
        "D14" {:path [:lahtotiedot :rakennusvaippa :ulkoovet :ala] :dp 1}
        "E14" {:path [:lahtotiedot :rakennusvaippa :ulkoovet :U] :dp 2}
        "F14" {:path [:lahtotiedot :rakennusvaippa :ulkoovet :UA] :dp 1}
        "G14" {:path [:lahtotiedot :rakennusvaippa :ulkoovet :osuus-lampohaviosta] :dp 0 :percent? true}
        "F15" {:path [:lahtotiedot :rakennusvaippa :kylmasillat-UA] :dp 1}
        "G15" {:path [:lahtotiedot :rakennusvaippa :kylmasillat-osuus-lampohaviosta] :dp 0 :percent? true}

        "D19" {:path [:lahtotiedot :ikkunat :pohjoinen :ala] :dp 1}
        "E19" {:path [:lahtotiedot :ikkunat :pohjoinen :U] :dp 2}
        "F19" {:path [:lahtotiedot :ikkunat :pohjoinen :g-ks] :dp 2}
        "D20" {:path [:lahtotiedot :ikkunat :koillinen :ala] :dp 1}
        "E20" {:path [:lahtotiedot :ikkunat :koillinen :U] :dp 2}
        "F20" {:path [:lahtotiedot :ikkunat :koillinen :g-ks] :dp 2}
        "D21" {:path [:lahtotiedot :ikkunat :ita :ala] :dp 1}
        "E21" {:path [:lahtotiedot :ikkunat :ita :U] :dp 2}
        "F21" {:path [:lahtotiedot :ikkunat :ita :g-ks] :dp 2}
        "D22" {:path [:lahtotiedot :ikkunat :kaakko :ala] :dp 1}
        "E22" {:path [:lahtotiedot :ikkunat :kaakko :U] :dp 2}
        "F22" {:path [:lahtotiedot :ikkunat :kaakko :g-ks] :dp 2}
        "D23" {:path [:lahtotiedot :ikkunat :etela :ala] :dp 1}
        "E23" {:path [:lahtotiedot :ikkunat :etela :U] :dp 2}
        "F23" {:path [:lahtotiedot :ikkunat :etela :g-ks] :dp 2}
        "D24" {:path [:lahtotiedot :ikkunat :lounas :ala] :dp 1}
        "E24" {:path [:lahtotiedot :ikkunat :lounas :U] :dp 2}
        "F24" {:path [:lahtotiedot :ikkunat :lounas :g-ks] :dp 2}
        "D25" {:path [:lahtotiedot :ikkunat :lansi :ala] :dp 1}
        "E25" {:path [:lahtotiedot :ikkunat :lansi :U] :dp 2}
        "F25" {:path [:lahtotiedot :ikkunat :lansi :g-ks] :dp 2}
        "D26" {:path [:lahtotiedot :ikkunat :luode :ala] :dp 1}
        "E26" {:path [:lahtotiedot :ikkunat :luode :U] :dp 2}
        "F26" {:path [:lahtotiedot :ikkunat :luode :g-ks] :dp 2}

        "D28" {:path [:lahtotiedot :ilmanvaihto :kuvaus-fi]}
        "D33" {:path [:lahtotiedot :ilmanvaihto :paaiv :tulo-poisto]}
        "E33" {:path [:lahtotiedot :ilmanvaihto :paaiv :sfp] :dp 2}
        "F33" {:path [:lahtotiedot :ilmanvaihto :paaiv :lampotilasuhde] :dp 0 :percent? true}
        "G33" {:path [:lahtotiedot :ilmanvaihto :paaiv :jaatymisenesto] :dp 2}
        "D34" {:path [:lahtotiedot :ilmanvaihto :erillispoistot :tulo-poisto]}
        "E34" {:path [:lahtotiedot :ilmanvaihto :erillispoistot :sfp] :dp 2}
        "D35" {:path [:lahtotiedot :ilmanvaihto :ivjarjestelma :tulo-poisto]}
        "E35" {:path [:lahtotiedot :ilmanvaihto :ivjarjestelma :sfp] :dp 2}
        "E36" {:path [:lahtotiedot :ilmanvaihto :lto-vuosihyotysuhde] :dp 0 :percent? true}

        "D38" {:path [:lahtotiedot :lammitys :kuvaus-fi]}

        "D43" {:path [:lahtotiedot :lammitys :tilat-ja-iv :tuoton-hyotysuhde] :dp 0 :percent? true}
        "E43" {:path [:lahtotiedot :lammitys :tilat-ja-iv :jaon-hyotysuhde] :dp 0 :percent? true}
        "F43" {:path [:lahtotiedot :lammitys :tilat-ja-iv :lampokerroin] :dp 1}
        "G43" {:path [:lahtotiedot :lammitys :tilat-ja-iv :apulaitteet] :dp 1}
        "D44" {:path [:lahtotiedot :lammitys :lammin-kayttovesi :tuoton-hyotysuhde] :dp 0 :percent? true}
        "E44" {:path [:lahtotiedot :lammitys :lammin-kayttovesi :jaon-hyotysuhde] :dp 0 :percent? true}
        "F44" {:path [:lahtotiedot :lammitys :lammin-kayttovesi :lampokerroin] :dp 1}
        "G44" {:path [:lahtotiedot :lammitys :lammin-kayttovesi :apulaitteet] :dp 1}

        "D50" {:path [:lahtotiedot :lammitys :takka :maara]}
        "E50" {:path [:lahtotiedot :lammitys :takka :tuotto] :dp 0}
        "D51" {:path [:lahtotiedot :lammitys :ilmanlampopumppu :maara]}
        "E51" {:path [:lahtotiedot :lammitys :ilmanlampopumppu :tuotto] :dp 0}

        "D55" {:path [:lahtotiedot :jaahdytysjarjestelma :jaahdytyskauden-painotettu-kylmakerroin] :dp 2}

        "D59" {:path [:lahtotiedot :lkvn-kaytto :kulutus-per-nelio] :dp 0}
        "E59" {:path [:lahtotiedot :lkvn-kaytto :vuosikulutus] :dp 0}

        "D63" {:f #(-> % sis-kuorma (get 0) first (format-number 0 true))}
        "E63" {:f #(-> % sis-kuorma (get 0) second :henkilot (format-number 1 false))}
        "F63" {:f #(-> % sis-kuorma (get 0) second :kuluttajalaitteet (format-number 1 false))}
        "G63" {:f #(-> % sis-kuorma (get 0) second :valaistus (format-number 1 false))}
        "D64" {:f #(-> % sis-kuorma (get 1) first (format-number 0 true))}
        "E64" {:f #(-> % sis-kuorma (get 1) second :henkilot (format-number 1 false))}
        "F64" {:f #(-> % sis-kuorma (get 1) second :kuluttajalaitteet (format-number 1 false))}
        "G64" {:f #(-> % sis-kuorma (get 1) second :valaistus (format-number 1 false))}
        "D65" {:f #(-> % sis-kuorma (get 2) first (format-number 0 true))}
        "E65" {:f #(-> % sis-kuorma (get 2) second :henkilot (format-number 1 false))}
        "F65" {:f #(-> % sis-kuorma (get 2) second :kuluttajalaitteet (format-number 1 false))}
        "G65" {:f #(-> % sis-kuorma (get 2) second :valaistus (format-number 1 false))}}
     3 {"D4" {:path [:perustiedot :alakayttotarkoitus-fi]}
        "D7" {:path [:perustiedot :valmistumisvuosi]}
        "D8" {:path [:lahtotiedot :lammitetty-nettoala] :dp 1}
        "D9" {:path [:tulokset :e-luku]}

        "D17" {:path [:tulokset :kaytettavat-energiamuodot :kaukolampo] :dp 0}
        "E17" {:path [:tulokset :kaytettavat-energiamuodot :kaukolampo-kerroin]}
        "F17" {:path [:tulokset :kaytettavat-energiamuodot :kaukolampo-kertoimella] :dp 0}
        "G17" {:path [:tulokset :kaytettavat-energiamuodot :kaukolampo-nettoala-kertoimella] :dp 0}
        "D18" {:path [:tulokset :kaytettavat-energiamuodot :sahko] :dp 0}
        "E18" {:path [:tulokset :kaytettavat-energiamuodot :sahko-kerroin]}
        "F18" {:path [:tulokset :kaytettavat-energiamuodot :sahko-kertoimella] :dp 0}
        "G18" {:path [:tulokset :kaytettavat-energiamuodot :sahko-nettoala-kertoimella] :dp 0}
        "D19" {:path [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine] :dp 0}
        "E19" {:path [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-kerroin]}
        "F19" {:path [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-kertoimella] :dp 0}
        "G19" {:path [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-nettoala-kertoimella] :dp 0}
        "D20" {:path [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys :dp 0]}
        "E20" {:path [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-kerroin]}
        "F20" {:path [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-kertoimella] :dp 0}
        "G20" {:path [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-nettoala-kertoimella] :dp 0}
        "D21" {:path [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine] :dp 0}
        "E21" {:path [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-kerroin]}
        "F21" {:path [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-kertoimella] :dp 0}
        "G21" {:path [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-nettoala-kertoimella] :dp 0}
        "D22" {:path [:tulokset :kaytettavat-energiamuodot :summa] :dp 0}
        "F22" {:path [:tulokset :kaytettavat-energiamuodot :kertoimella-summa] :dp 0}
        "G22" {:path [:tulokset :kaytettavat-energiamuodot :nettoala-kertoimella-summa] :dp 0}

        "E28" {:path [:tulokset :uusiutuvat-omavaraisenergiat :aurinkosahko] :dp 0}
        "F28" {:path [:tulokset :uusiutuvat-omavaraisenergiat :aurinkosahko-nettoala] :dp 0}
        "E29" {:path [:tulokset :uusiutuvat-omavaraisenergiat :aurinkolampo] :dp 0}
        "F29" {:path [:tulokset :uusiutuvat-omavaraisenergiat :aurinkolampo-nettoala] :dp 0}
        "E30" {:path [:tulokset :uusiutuvat-omavaraisenergiat :tuulisahko] :dp 0}
        "F30" {:path [:tulokset :uusiutuvat-omavaraisenergiat :tuulisahko-nettoala] :dp 0}
        "E31" {:path [:tulokset :uusiutuvat-omavaraisenergiat :lampopumppu] :dp 0}
        "F31" {:path [:tulokset :uusiutuvat-omavaraisenergiat :lampopumppu-nettoala] :dp 0}
        "E32" {:path [:tulokset :uusiutuvat-omavaraisenergiat :muusahko] :dp 0}
        "F32" {:path [:tulokset :uusiutuvat-omavaraisenergiat :muusahko-nettoala] :dp 0}
        "E33" {:path [:tulokset :uusiutuvat-omavaraisenergiat :muulampo] :dp 0}
        "F33" {:path [:tulokset :uusiutuvat-omavaraisenergiat :muulampo-nettoala] :dp 0}

        "E41" {:path [:tulokset :tekniset-jarjestelmat :tilojen-lammitys :sahko] :dp 1}
        "F41" {:path [:tulokset :tekniset-jarjestelmat :tilojen-lammitys :lampo] :dp 1}
        "E42" {:path [:tulokset :tekniset-jarjestelmat :tuloilman-lammitys :sahko] :dp 1}
        "F42" {:path [:tulokset :tekniset-jarjestelmat :tuloilman-lammitys :lampo] :dp 1}
        "E43" {:path [:tulokset :tekniset-jarjestelmat :kayttoveden-valmistus :sahko] :dp 1}
        "F43" {:path [:tulokset :tekniset-jarjestelmat :kayttoveden-valmistus :lampo] :dp 1}
        "E44" {:path [:tulokset :tekniset-jarjestelmat :iv-sahko] :dp 1}
        "E45" {:path [:tulokset :tekniset-jarjestelmat :jaahdytys :sahko] :dp 1}
        "F45" {:path [:tulokset :tekniset-jarjestelmat :jaahdytys :lampo] :dp 1}
        "G45" {:path [:tulokset :tekniset-jarjestelmat :jaahdytys :kaukojaahdytys] :dp 1}
        "E46" {:path [:tulokset :tekniset-jarjestelmat :kuluttajalaitteet-ja-valaistus-sahko] :dp 1}

        "E47" {:path [:tulokset :tekniset-jarjestelmat :sahko-summa] :dp 1}
        "F47" {:path [:tulokset :tekniset-jarjestelmat :lampo-summa] :dp 1}
        "G47" {:path [:tulokset :tekniset-jarjestelmat :kaukojaahdytys-summa] :dp 1}

        "E54" {:path [:tulokset :nettotarve :tilojen-lammitys-vuosikulutus] :dp 0}
        "F54" {:path [:tulokset :nettotarve :tilojen-lammitys-vuosikulutus-nettoala] :dp 0}
        "E55" {:path [:tulokset :nettotarve :ilmanvaihdon-lammitys-vuosikulutus] :dp 0}
        "F55" {:path [:tulokset :nettotarve :ilmanvaihdon-lammitys-vuosikulutus-nettoala] :dp 0}
        "E56" {:path [:tulokset :nettotarve :kayttoveden-valmistus-vuosikulutus] :dp 0}
        "F56" {:path [:tulokset :nettotarve :kayttoveden-valmistus-vuosikulutus-nettoala] :dp 0}
        "E57" {:path [:tulokset :nettotarve :jaahdytys-vuosikulutus] :dp 0}
        "F57" {:path [:tulokset :nettotarve :jaahdytys-vuosikulutus-nettoala] :dp 0}

        "E66" {:path [:tulokset :lampokuormat :aurinko] :dp 0}
        "F66" {:path [:tulokset :lampokuormat :aurinko-nettoala] :dp 0}
        "E67" {:path [:tulokset :lampokuormat :ihmiset] :dp 0}
        "F67" {:path [:tulokset :lampokuormat :ihmiset-nettoala] :dp 0}
        "E68" {:path [:tulokset :lampokuormat :kuluttajalaitteet] :dp 0}
        "F68" {:path [:tulokset :lampokuormat :kuluttajalaitteet-nettoala] :dp 0}
        "E69" {:path [:tulokset :lampokuormat :valaistus] :dp 0}
        "F69" {:path [:tulokset :lampokuormat :valaistus-nettoala] :dp 0}
        "E70" {:path [:tulokset :lampokuormat :kvesi] :dp 0}
        "F70" {:path [:tulokset :lampokuormat :kvesi-nettoala] :dp 0}

        "E74" {:path [:tulokset :laskentatyokalu]}}
     4 {"C7" {:f #(str "Lämmitetty nettoala "
                       (-> %
                           :lahtotiedot
                           :lammitetty-nettoala
                           (format-number 1 false))
                       " m²")}

        "H12" {:path [:toteutunut-ostoenergiankulutus :ostettu-energia :kaukolampo-vuosikulutus] :dp 0}
        "I12" {:path [:toteutunut-ostoenergiankulutus :ostettu-energia :kaukolampo-vuosikulutus-nettoala] :dp 0}
        "H14" {:path [:toteutunut-ostoenergiankulutus :ostettu-energia :kokonaissahko-vuosikulutus] :dp 0}
        "I14" {:path [:toteutunut-ostoenergiankulutus :ostettu-energia :kokonaissahko-vuosikulutus-nettoala] :dp 0}
        "H16" {:path [:toteutunut-ostoenergiankulutus :ostettu-energia :kiinteistosahko-vuosikulutus] :dp 0}
        "I16" {:path [:toteutunut-ostoenergiankulutus :ostettu-energia :kiinteistosahko-vuosikulutus-nettoala] :dp 0}
        "H17" {:path [:toteutunut-ostoenergiankulutus :ostettu-energia :kayttajasahko-vuosikulutus] :dp 0}
        "I17" {:path [:toteutunut-ostoenergiankulutus :ostettu-energia :kayttajasahko-vuosikulutus-nettoala] :dp 0}
        "H19" {:path [:toteutunut-ostoenergiankulutus :ostettu-energia :kaukojaahdytys-vuosikulutus] :dp 0}
        "I19" {:path [:toteutunut-ostoenergiankulutus :ostettu-energia :kaukojaahdytys-vuosikulutus-nettoala] :dp 0}

        "E23" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :kevyt-polttooljy] :dp 0}
        "H23" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :kevyt-polttooljy-kwh] :dp 0}
        "I23" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :kevyt-polttooljy-kwh-nettoala] :dp 0}
        "E24" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-havu-sekapuu] :dp 0}
        "H24" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-havu-sekapuu-kwh] :dp 0}
        "I24" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-havu-sekapuu-kwh-nettoala] :dp 0}
        "E25" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-koivu] :dp 0}
        "H25" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-koivu-kwh] :dp 0}
        "I25" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-koivu-kwh-nettoala] :dp 0}
        "E26" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :puupelletit] :dp 0}
        "H26" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :puupelletit-kwh] :dp 0}
        "I26" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :puupelletit-kwh-nettoala] :dp 0}

        "C27" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 0 :nimi]}
        "E27" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 0 :maara-vuodessa] :dp 0}
        "F27" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 0 :yksikko]}
        "G27" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 0 :muunnoskerroin]}
        "H27" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 0 :kwh] :dp 0}
        "I27" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 0 :kwh-nettoala] :dp 0}
        "C28" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 1 :nimi]}
        "E28" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 1 :maara-vuodessa] :dp 0}
        "F28" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 1 :yksikko]}
        "G28" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 1 :muunnoskerroin]}
        "H28" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 1 :kwh] :dp 0}
        "I28" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 1 :kwh-nettoala] :dp 0}

        "H38" {:path [:toteutunut-ostoenergiankulutus :sahko-vuosikulutus-yhteensa] :dp 0}
        "I38" {:path [:toteutunut-ostoenergiankulutus :sahko-vuosikulutus-yhteensa-nettoala] :dp 0}
        "H40" {:path [:toteutunut-ostoenergiankulutus :kaukolampo-vuosikulutus-yhteensa] :dp 0}
        "I40" {:path [:toteutunut-ostoenergiankulutus :kaukolampo-vuosikulutus-yhteensa-nettoala] :dp 0}
        "H42" {:path [:toteutunut-ostoenergiankulutus :polttoaineet-vuosikulutus-yhteensa] :dp 0}
        "I42" {:path [:toteutunut-ostoenergiankulutus :polttoaineet-vuosikulutus-yhteensa-nettoala] :dp 0}
        "H44" {:path [:toteutunut-ostoenergiankulutus :kaukojaahdytys-vuosikulutus-yhteensa] :dp 0}
        "I44" {:path [:toteutunut-ostoenergiankulutus :kaukojaahdytys-vuosikulutus-yhteensa-nettoala] :dp 0}
        "H46" {:path [:toteutunut-ostoenergiankulutus :summa] :dp 0}
        "I46" {:path [:toteutunut-ostoenergiankulutus :summa-nettoala] :dp 0}}
     5 {"B5" {:path [:huomiot :ymparys :teksti-fi]}
        "C12" {:path [:huomiot :ymparys :toimenpide 0 :nimi-fi]}
        "C13" {:path [:huomiot :ymparys :toimenpide 1 :nimi-fi]}
        "C14" {:path [:huomiot :ymparys :toimenpide 2 :nimi-fi]}
        "C17" {:path [:huomiot :ymparys :toimenpide 0 :lampo] :dp 0}
        "D17" {:path [:huomiot :ymparys :toimenpide 0 :sahko] :dp 0}
        "E17" {:path [:huomiot :ymparys :toimenpide 0 :jaahdytys] :dp 0}
        "F17" {:path [:huomiot :ymparys :toimenpide 0 :eluvun-muutos] :dp 0}
        "C18" {:path [:huomiot :ymparys :toimenpide 1 :lampo] :dp 0}
        "D18" {:path [:huomiot :ymparys :toimenpide 1 :sahko] :dp 0}
        "E18" {:path [:huomiot :ymparys :toimenpide 1 :jaahdytys] :dp 0}
        "F18" {:path [:huomiot :ymparys :toimenpide 1 :eluvun-muutos] :dp 0}
        "C19" {:path [:huomiot :ymparys :toimenpide 2 :lampo] :dp 0}
        "D19" {:path [:huomiot :ymparys :toimenpide 2 :sahko] :dp 0}
        "E19" {:path [:huomiot :ymparys :toimenpide 2 :jaahdytys] :dp 0}
        "F19" {:path [:huomiot :ymparys :toimenpide 2 :eluvun-muutos] :dp 0}

        "B21" {:path [:huomiot :alapohja-ylapohja :teksti-fi]}
        "C28" {:path [:huomiot :alapohja-ylapohja :toimenpide 0 :nimi-fi]}
        "C29" {:path [:huomiot :alapohja-ylapohja :toimenpide 1 :nimi-fi]}
        "C30" {:path [:huomiot :alapohja-ylapohja :toimenpide 2 :nimi-fi]}
        "C33" {:path [:huomiot :alapohja-ylapohja :toimenpide 0 :lampo] :dp 0}
        "D33" {:path [:huomiot :alapohja-ylapohja :toimenpide 0 :sahko] :dp 0}
        "E33" {:path [:huomiot :alapohja-ylapohja :toimenpide 0 :jaahdytys] :dp 0}
        "F33" {:path [:huomiot :alapohja-ylapohja :toimenpide 0 :eluvun-muutos] :dp 0}
        "C34" {:path [:huomiot :alapohja-ylapohja :toimenpide 1 :lampo] :dp 0}
        "D34" {:path [:huomiot :alapohja-ylapohja :toimenpide 1 :sahko] :dp 0}
        "E34" {:path [:huomiot :alapohja-ylapohja :toimenpide 1 :jaahdytys] :dp 0}
        "F34" {:path [:huomiot :alapohja-ylapohja :toimenpide 1 :eluvun-muutos] :dp 0}
        "C35" {:path [:huomiot :alapohja-ylapohja :toimenpide 2 :lampo] :dp 0}
        "D35" {:path [:huomiot :alapohja-ylapohja :toimenpide 2 :sahko] :dp 0}
        "E35" {:path [:huomiot :alapohja-ylapohja :toimenpide 2 :jaahdytys] :dp 0}
        "F35" {:path [:huomiot :alapohja-ylapohja :toimenpide 2 :eluvun-muutos] :dp 0}

        "B37" {:path [:huomiot :lammitys :teksti-fi]}
        "C44" {:path [:huomiot :lammitys :toimenpide 0 :nimi-fi]}
        "C45" {:path [:huomiot :lammitys :toimenpide 1 :nimi-fi]}
        "C46" {:path [:huomiot :lammitys :toimenpide 2 :nimi-fi]}
        "C49" {:path [:huomiot :lammitys :toimenpide 0 :lampo] :dp 0}
        "D49" {:path [:huomiot :lammitys :toimenpide 0 :sahko] :dp 0}
        "E49" {:path [:huomiot :lammitys :toimenpide 0 :jaahdytys] :dp 0}
        "F49" {:path [:huomiot :lammitys :toimenpide 0 :eluvun-muutos] :dp 0}
        "C50" {:path [:huomiot :lammitys :toimenpide 1 :lampo] :dp 0}
        "D50" {:path [:huomiot :lammitys :toimenpide 1 :sahko] :dp 0}
        "E50" {:path [:huomiot :lammitys :toimenpide 1 :jaahdytys] :dp 0}
        "F50" {:path [:huomiot :lammitys :toimenpide 1 :eluvun-muutos] :dp 0}
        "C51" {:path [:huomiot :lammitys :toimenpide 2 :lampo] :dp 0}
        "D51" {:path [:huomiot :lammitys :toimenpide 2 :sahko] :dp 0}
        "E51" {:path [:huomiot :lammitys :toimenpide 2 :jaahdytys] :dp 0}
        "F51" {:path [:huomiot :lammitys :toimenpide 2 :eluvun-muutos] :dp 0}}
     6 {"B3" {:path [:huomiot :iv-ilmastointi :teksti-fi]}
        "C11" {:path [:huomiot :iv-ilmastointi :toimenpide 0 :nimi-fi]}
        "C12" {:path [:huomiot :iv-ilmastointi :toimenpide 1 :nimi-fi]}
        "C13" {:path [:huomiot :iv-ilmastointi :toimenpide 2 :nimi-fi]}
        "C16" {:path [:huomiot :iv-ilmastointi :toimenpide 0 :lampo] :dp 0}
        "D16" {:path [:huomiot :iv-ilmastointi :toimenpide 0 :sahko] :dp 0}
        "E16" {:path [:huomiot :iv-ilmastointi :toimenpide 0 :jaahdytys] :dp 0}
        "F16" {:path [:huomiot :iv-ilmastointi :toimenpide 0 :eluvun-muutos] :dp 0}
        "C17" {:path [:huomiot :iv-ilmastointi :toimenpide 1 :lampo] :dp 0}
        "D17" {:path [:huomiot :iv-ilmastointi :toimenpide 1 :sahko] :dp 0}
        "E17" {:path [:huomiot :iv-ilmastointi :toimenpide 1 :jaahdytys] :dp 0}
        "F17" {:path [:huomiot :iv-ilmastointi :toimenpide 1 :eluvun-muutos] :dp 0}
        "C18" {:path [:huomiot :iv-ilmastointi :toimenpide 2 :lampo] :dp 0}
        "D18" {:path [:huomiot :iv-ilmastointi :toimenpide 2 :sahko] :dp 0}
        "E18" {:path [:huomiot :iv-ilmastointi :toimenpide 2 :jaahdytys] :dp 0}
        "F18" {:path [:huomiot :iv-ilmastointi :toimenpide 2 :eluvun-muutos] :dp 0}

        "B20" {:path [:huomiot :valaistus-muut :teksti-fi]}
        "C28" {:path [:huomiot :valaistus-muut :toimenpide 0 :nimi-fi]}
        "C29" {:path [:huomiot :valaistus-muut :toimenpide 1 :nimi-fi]}
        "C30" {:path [:huomiot :valaistus-muut :toimenpide 2 :nimi-fi]}
        "C33" {:path [:huomiot :valaistus-muut :toimenpide 0 :lampo] :dp 0}
        "D33" {:path [:huomiot :valaistus-muut :toimenpide 0 :sahko] :dp 0}
        "E33" {:path [:huomiot :valaistus-muut :toimenpide 0 :jaahdytys] :dp 0}
        "F33" {:path [:huomiot :valaistus-muut :toimenpide 0 :eluvun-muutos] :dp 0}
        "C34" {:path [:huomiot :valaistus-muut :toimenpide 1 :lampo] :dp 0}
        "D34" {:path [:huomiot :valaistus-muut :toimenpide 1 :sahko] :dp 0}
        "E34" {:path [:huomiot :valaistus-muut :toimenpide 1 :jaahdytys] :dp 0}
        "F34" {:path [:huomiot :valaistus-muut :toimenpide 1 :eluvun-muutos] :dp 0}
        "C35" {:path [:huomiot :valaistus-muut :toimenpide 2 :lampo] :dp 0}
        "D35" {:path [:huomiot :valaistus-muut :toimenpide 2 :sahko] :dp 0}
        "E35" {:path [:huomiot :valaistus-muut :toimenpide 2 :jaahdytys] :dp 0}
        "F35" {:path [:huomiot :valaistus-muut :toimenpide 2 :eluvun-muutos] :dp 0}

        "B37" {:path [:huomiot :suositukset-fi]}}
     7 {"B3" {:path [:lisamerkintoja-fi]}}}))

(defn fill-xlsx-template [complete-energiatodistus draft?]
  (with-open [is (-> xlsx-template-path io/resource io/input-stream)]
    (let [loaded-xlsx (xlsx/load-xlsx is)
          sheets (map #(xlsx/get-sheet loaded-xlsx %) (range sheet-count))
          path (->> (java.util.UUID/randomUUID)
                    .toString
                    (format "energiatodistus-%s.xlsx")
                    (str tmp-dir))]
      (doseq [[sheet sheet-mappings] (mappings)]
        (doseq [[cell {:keys [path f dp percent?]}] sheet-mappings]
          (xlsx/set-cell-value-at (nth sheets sheet)
                                  cell
                                  (if f
                                    (f complete-energiatodistus)
                                    (let [v (get-in complete-energiatodistus path)]
                                      (if (number? v)
                                        (format-number v dp percent?)
                                        v))))))
      (io/make-parents path)
      (xlsx/save-xlsx loaded-xlsx path)
      path)))

;; Uses current Libreoffice export settings. Make sure they are set
;; for PDFA-2B.
(defn xlsx->pdf [path]
  "Uses current LibreOffice export settings. Make sure they are set
   to PDFA-2B. Path must be a path on disk, not on classpath."
  (let [file (io/file path)
        filename (.getName file)
        dir (.getParent file)
        result-pdf (str/replace path #".xlsx$" ".pdf")
        {:keys [exit err] :as sh-result} (shell/sh "libreoffice"
                                                   "--headless"
                                                   "--convert-to"
                                                   "pdf"
                                                   filename
                                                   :dir
                                                   dir)]
    (if (and (zero? exit) (str/blank? err) (.exists (io/as-file result-pdf)))
      result-pdf
      (throw (ex-info "XLSX to PDF conversion failed"
                (assoc sh-result
                  :type :xlsx-pdf-conversion-failure
                  :xlsx filename
                  :pdf-result? (.exists (io/as-file result-pdf))))))))

(defn- add-watermark [pdf-path]
  (with-open [watermark (PDDocument/load (-> watermark-path-fi io/resource io/input-stream))
              overlay   (doto (Overlay.)
                          (.setInputFile pdf-path)
                          (.setDefaultOverlayPDF watermark)
                          (.setOverlayPosition Overlay$Position/FOREGROUND))
              result    (.overlay overlay (HashMap.))]
    (.save result pdf-path)
    pdf-path))

(defn generate-pdf-as-file [complete-energiatodistus draft?]
  (let [xlsx-path (fill-xlsx-template complete-energiatodistus draft?)
        pdf-path (xlsx->pdf xlsx-path)]
    (io/delete-file xlsx-path)
    (if draft?
      (add-watermark pdf-path)
      pdf-path)))

(defn generate-pdf-as-input-stream [energiatodistus draft?]
  (let [pdf-path (generate-pdf-as-file energiatodistus draft?)
        is (io/input-stream pdf-path)]
    (io/delete-file pdf-path)
    is))

(defn pdf-file-id [id]
  (when id (str "energiatodistus-" id)))

(defn find-existing-pdf [db id]
  (->> id
       pdf-file-id
       (file-service/find-file db)
       :content
       io/input-stream))

(defn find-energiatodistus-pdf [db whoami id]
  (when-let [{:keys [allekirjoitusaika] :as complete-energiatodistus}
             (energiatodistus-service/find-complete-energiatodistus db whoami id)]
    (if allekirjoitusaika
      (find-existing-pdf db id)
      (generate-pdf-as-input-stream complete-energiatodistus true))))

(defn do-when-signing [{:keys [tila-id]} f]
  (case (energiatodistus-service/tila-key tila-id)
    :in-signing (f)
    :draft :not-in-signing
    :deleted :not-in-signing
    :already-signed))

(defn signature-as-png [path laatija-fullname]
  (let [now (Instant/now)
        width (max 125 (* (count laatija-fullname) 6))
        img (BufferedImage. width 30 BufferedImage/TYPE_INT_ARGB)
        g (.getGraphics img)]
    (doto (.getGraphics img)
      (.setFont (Font. Font/SANS_SERIF Font/TRUETYPE_FONT 10))
      (.setColor Color/BLACK)
      (.drawString laatija-fullname 2 10)
      (.drawString (.format time-formatter now) 2 25)
      (.dispose))
    (ImageIO/write img "PNG" (io/file path))))

(defn find-energiatodistus-digest [db id]
  (when-let [{:keys [laatija-fullname] :as complete-energiatodistus}
             (energiatodistus-service/find-complete-energiatodistus db id)]
    (do-when-signing
     complete-energiatodistus
     #(let [pdf-path (generate-pdf-as-file complete-energiatodistus false)
            signable-pdf-path (str/replace pdf-path #".pdf" "-signable.pdf")
            signature-png-path (str/replace pdf-path #".pdf" "-signature.png")
            _ (signature-as-png signature-png-path laatija-fullname)
            signable-pdf-path (puumerkki/add-watermarked-signature-space
                               pdf-path
                               signable-pdf-path
                               laatija-fullname
                               signature-png-path
                               75
                               666)
            signable-pdf-data (puumerkki/read-file signable-pdf-path)
            digest (puumerkki/compute-base64-pkcs signable-pdf-data)
            file-id (pdf-file-id id)]
        (file-service/upsert-file-from-bytes db
                                             file-id
                                             (str file-id ".pdf")
                                             signable-pdf-data)
        (io/delete-file pdf-path)
        (io/delete-file signable-pdf-path)
        (io/delete-file signature-png-path)
        {:digest digest}))))

(defn sign-energiatodistus-pdf [db id signature-and-chain]
  (when-let [energiatodistus
             (energiatodistus-service/find-energiatodistus db id)]
    (do-when-signing
     energiatodistus
     #(try (let [file-id (pdf-file-id id)
                 {:keys [filename content] :as file-info}
                 (file-service/find-file db file-id)
                 content-bytes (.readAllBytes content)
                 pkcs7 (puumerkki/make-pkcs7 signature-and-chain content-bytes)
                 filename (str file-id ".pdf")]
             (do
               (->> (puumerkki/write-signature! content-bytes pkcs7)
                    (file-service/upsert-file-from-bytes db
                                                         file-id
                                                         filename))
               filename))
           (catch java.lang.ArrayIndexOutOfBoundsException e :pdf-exists)))))
