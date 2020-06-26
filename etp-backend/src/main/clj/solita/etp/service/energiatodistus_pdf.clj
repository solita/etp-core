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
        "K9" #(str (-> % :perustiedot :postinumero) " " "Helsinki")
        "K12" {:path [:perustiedot :rakennustunnus]}
        "K13" {:path [:perustiedot :valmistumisvuosi]}

        "K14" {:path [:perustiedot :alakayttotarkoitus-fi]}
        "K16" {:path [:id]}

        "D19" (fn [energiatodistus]
                (if (= (-> energiatodistus :perustiedot :laatimisvaihe) 0)
                  "☒ Uudelle rakennukselle rakennuslupaa haettaessa"
                  "☐ Uudelle rakennukselle rakennuslupaa haettaessa"))
        "D20" (fn [energiatodistus]
                (if (= (-> energiatodistus :perustiedot :laatimisvaihe) 1)
                  "☒ Uudelle rakennukselle käyttöönottovaiheessa"
                  "☐ Uudelle rakennukselle käyttöönottovaiheessa"))
        "D21" (fn [energiatodistus]
                (if (= (-> energiatodistus :perustiedot :laatimisvaihe) 2)
                  "☒ Olemassa olevalle rakennukselle, havainnointikäynnin päivämäärä:"
                  "☐ Olemassa olevalle rakennukselle, havainnointikäynnin päivämäärä:"))

        "M21" (fn [energiatodistus]
                (some->> energiatodistus :perustiedot :havainnointikaynti (.format date-formatter)))

        ;; TODO M37 E-luvun vaatimus
        "M36" {:path [:tulokset :e-luku]}

        "B42" {:path [:laatija-fullname]}
        "J42" {:path [:perustiedot :yritys :nimi]}

        "B50" (fn [_] (.format date-formatter today))
        "K50" (fn [_] (.format date-formatter (.plusYears today 10)))}
     1 {"F5" #(format "%s m²" (-> % :lahtotiedot :lammitetty-nettoala))
        "F6" {:path [:lahtotiedot :lammitys :kuvaus-fi]}
        "F7" {:path [:lahtotiedot :ilmanvaihto :kuvaus-fi]}
        "F14" {:path [:tulokset :kaytettavat-energiamuodot :kaukolampo]}
        "G14" {:path [:tulokset :kaytettavat-energiamuodot :kaukolampo-nettoala]}
        "H14" {:path [:tulokset :kaytettavat-energiamuodot :kaukolampo-kerroin]}
        "I14" {:path [:tulokset :kaytettavat-energiamuodot :kaukolampo-nettoala-kertoimella]}
        "F15" {:path [:tulokset :kaytettavat-energiamuodot :sahko]}
        "G15" {:path [:tulokset :kaytettavat-energiamuodot :sahko-nettoala]}
        "H15" {:path [:tulokset :kaytettavat-energiamuodot :sahko-kerroin]}
        "I15" {:path [:tulokset :kaytettavat-energiamuodot :sahko-nettoala-kertoimella]}
        "F16" {:path [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine]}
        "G16" {:path [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-nettoala]}
        "H16" {:path [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-kerroin]}
        "I16" {:path [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-nettoala-kertoimella]}
        "F17" {:path [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine]}
        "G17" {:path [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-nettoala]}
        "H17" {:path [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-kerroin]}
        "I17" {:path [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-nettoala-kertoimella]}
        "F18" {:path [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys]}
        "G18" {:path [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-nettoala]}
        "H18" {:path [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-kerroin]}
        "I18" {:path [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-nettoala-kertoimella]}

        "I20" {:path [:tulokset :e-luku]}

        ;; TODO Käytetty E-luvun luokittelu asteikko

        ;; TODO Luokkien rajat asteikolla

        ;; TODO Tämän rakennuksen energiatehokkuusluokka

        "C38" {:path [:perustiedot :keskeiset-suositukset-fi]}}
     2 {"D4" {:path [:perustiedot :alakayttotarkoitus-fi]}
        "D5" {:path [:perustiedot :valmistumisvuosi]}
        "F5" {:path [:lahtotiedot :lammitetty-nettoala]}
        "D7" {:path [:lahtotiedot :rakennusvaippa :ilmanvuotoluku]}

        "D10" {:path [:lahtotiedot :rakennusvaippa :ulkoseinat :ala]}
        "E10" {:path [:lahtotiedot :rakennusvaippa :ulkoseinat :U]}
        "F10" {:path [:lahtotiedot :rakennusvaippa :ulkoseinat :UA]}
        "G10" {:path [:lahtotiedot :rakennusvaippa :ulkoseinat :osuus-lampohaviosta]}
        "D11" {:path [:lahtotiedot :rakennusvaippa :ylapohja :ala]}
        "E11" {:path [:lahtotiedot :rakennusvaippa :ylapohja :U]}
        "F11" {:path [:lahtotiedot :rakennusvaippa :ylapohja :UA]}
        "G11" {:path [:lahtotiedot :rakennusvaippa :ylapohja :osuus-lampohaviosta]}
        "D12" {:path [:lahtotiedot :rakennusvaippa :alapohja :ala]}
        "E12" {:path [:lahtotiedot :rakennusvaippa :alapohja :U]}
        "F12" {:path [:lahtotiedot :rakennusvaippa :alapohja :UA]}
        "G12" {:path [:lahtotiedot :rakennusvaippa :alapohja :osuus-lampohaviosta]}
        "D13" {:path [:lahtotiedot :rakennusvaippa :ikkunat :ala]}
        "E13" {:path [:lahtotiedot :rakennusvaippa :ikkunat :U]}
        "F13" {:path [:lahtotiedot :rakennusvaippa :ikkunat :UA]}
        "G13" {:path [:lahtotiedot :rakennusvaippa :ikkunat :osuus-lampohaviosta]}
        "D14" {:path [:lahtotiedot :rakennusvaippa :ulkoovet :ala]}
        "E14" {:path [:lahtotiedot :rakennusvaippa :ulkoovet :U]}
        "F14" {:path [:lahtotiedot :rakennusvaippa :ulkoovet :UA]}
        "G14" {:path [:lahtotiedot :rakennusvaippa :ulkoovet :osuus-lampohaviosta]}
        "F15" {:path [:lahtotiedot :rakennusvaippa :kylmasillat-UA]}
        "G15" {:path [:lahtotiedot :rakennusvaippa :kylmasillat-osuus-lampohaviosta]}

        "D19" {:path [:lahtotiedot :ikkunat :pohjoinen :ala]}
        "E19" {:path [:lahtotiedot :ikkunat :pohjoinen :U]}
        "F19" {:path [:lahtotiedot :ikkunat :pohjoinen :g-ks]}
        "D20" {:path [:lahtotiedot :ikkunat :koillinen :ala]}
        "E20" {:path [:lahtotiedot :ikkunat :koillinen :U]}
        "F20" {:path [:lahtotiedot :ikkunat :koillinen :g-ks]}
        "D21" {:path [:lahtotiedot :ikkunat :ita :ala]}
        "E21" {:path [:lahtotiedot :ikkunat :ita :U]}
        "F21" {:path [:lahtotiedot :ikkunat :ita :g-ks]}
        "D22" {:path [:lahtotiedot :ikkunat :kaakko :ala]}
        "E22" {:path [:lahtotiedot :ikkunat :kaakko :U]}
        "F22" {:path [:lahtotiedot :ikkunat :kaakko :g-ks]}
        "D23" {:path [:lahtotiedot :ikkunat :etela :ala]}
        "E23" {:path [:lahtotiedot :ikkunat :etela :U]}
        "F23" {:path [:lahtotiedot :ikkunat :etela :g-ks]}
        "D24" {:path [:lahtotiedot :ikkunat :lounas :ala]}
        "E24" {:path [:lahtotiedot :ikkunat :lounas :U]}
        "F24" {:path [:lahtotiedot :ikkunat :lounas :g-ks]}
        "D25" {:path [:lahtotiedot :ikkunat :lansi :ala]}
        "E25" {:path [:lahtotiedot :ikkunat :lansi :U]}
        "F25" {:path [:lahtotiedot :ikkunat :lansi :g-ks]}
        "D26" {:path [:lahtotiedot :ikkunat :luode :ala]}
        "E26" {:path [:lahtotiedot :ikkunat :luode :U]}
        "F26" {:path [:lahtotiedot :ikkunat :luode :g-ks]}

        "D28" {:path [:lahtotiedot :ilmanvaihto :kuvaus-fi]}
        "D33" {:path [:lahtotiedot :ilmanvaihto :paaiv :tulo-poisto]}
        "E33" {:path [:lahtotiedot :ilmanvaihto :paaiv :sfp]}
        "F33" {:path [:lahtotiedot :ilmanvaihto :paaiv :lampotilasuhde]}
        "G33" {:path [:lahtotiedot :ilmanvaihto :paaiv :jaatymisenesto]}
        "D34" {:path [:lahtotiedot :ilmanvaihto :erillispoistot :tulo-poisto]}
        "E34" {:path [:lahtotiedot :ilmanvaihto :erillispoistot :sfp]}
        "D35" {:path [:lahtotiedot :ilmanvaihto :ivjarjestelma :tulo-poisto]}
        "E35" {:path [:lahtotiedot :ilmanvaihto :ivjarjestelma :sfp]}
        "E36" {:path [:lahtotiedot :ilmanvaihto :lto-vuosihyotysuhde]}

        "D38" {:path [:lahtotiedot :lammitys :kuvaus-fi]}

        "D43" {:path [:lahtotiedot :lammitys :tilat-ja-iv :tuoton-hyotysuhde]}
        "E43" {:path [:lahtotiedot :lammitys :tilat-ja-iv :jaon-hyotysuhde]}
        "F43" {:path [:lahtotiedot :lammitys :tilat-ja-iv :lampokerroin]}
        "G43" {:path [:lahtotiedot :lammitys :tilat-ja-iv :apulaitteet]}
        "D44" {:path [:lahtotiedot :lammitys :lammin-kayttovesi :tuoton-hyotysuhde]}
        "E44" {:path [:lahtotiedot :lammitys :lammin-kayttovesi :jaon-hyotysuhde]}
        "F44" {:path [:lahtotiedot :lammitys :lammin-kayttovesi :lampokerroin]}
        "G44" {:path [:lahtotiedot :lammitys :lammin-kayttovesi :apulaitteet]}

        "D50" {:path [:lahtotiedot :lammitys :takka :maara]}
        "E50" {:path [:lahtotiedot :lammitys :takka :tuotto]}
        "D51" {:path [:lahtotiedot :lammitys :ilmanlampopumppu :maara]}
        "E51" {:path [:lahtotiedot :lammitys :ilmanlampopumppu :tuotto]}

        "D55" {:path [:lahtotiedot :jaahdytysjarjestelma :jaahdytyskauden-painotettu-kylmakerroin]}

        "D59" {:path [:lahtotiedot :lkvn-kaytto :kulutus-per-nelio]}
        "E59" {:path [:lahtotiedot :lkvn-kaytto :vuosikulutus]}

        "D63" #(-> % sis-kuorma (get 0) first)
        "E63" #(-> % sis-kuorma (get 0) second :henkilot)
        "F63" #(-> % sis-kuorma (get 0) second :kuluttajalaitteet)
        "G63" #(-> % sis-kuorma (get 0) second :valaistus)
        "D64" #(-> % sis-kuorma (get 1) first)
        "E64" #(-> % sis-kuorma (get 1) second :henkilot)
        "F64" #(-> % sis-kuorma (get 1) second :kuluttajalaitteet)
        "G64" #(-> % sis-kuorma (get 1) second :valaistus)
        "D65" #(-> % sis-kuorma (get 2) first)
        "E65" #(-> % sis-kuorma (get 2) second :henkilot)
        "F65" #(-> % sis-kuorma (get 2) second :kuluttajalaitteet)
        "G65" #(-> % sis-kuorma (get 2) second :valaistus)}
     3 {"D4" {:path [:perustiedot :alakayttotarkoitus-fi]}
        "D7" {:path [:perustiedot :valmistumisvuosi]}
        "D8" {:path [:lahtotiedot :lammitetty-nettoala]}
        "D9" {:path [:tulokset :e-luku]}

        "D17" {:path [:tulokset :kaytettavat-energiamuodot :kaukolampo]}
        "E17" {:path [:tulokset :kaytettavat-energiamuodot :kaukolampo-kerroin]}
        "F17" {:path [:tulokset :kaytettavat-energiamuodot :kaukolampo-kertoimella]}
        "G17" {:path [:tulokset :kaytettavat-energiamuodot :kaukolampo-nettoala-kertoimella]}
        "D18" {:path [:tulokset :kaytettavat-energiamuodot :sahko]}
        "E18" {:path [:tulokset :kaytettavat-energiamuodot :sahko-kerroin]}
        "F18" {:path [:tulokset :kaytettavat-energiamuodot :sahko-kertoimella]}
        "G18" {:path [:tulokset :kaytettavat-energiamuodot :sahko-nettoala-kertoimella]}
        "D19" {:path [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine]}
        "E19" {:path [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-kerroin]}
        "F19" {:path [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-kertoimella]}
        "G19" {:path [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-nettoala-kertoimella]}

        "D20" {:path [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys]}
        "E20" {:path [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-kerroin]}
        "F20" {:path [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-kertoimella]}
        "G20" {:path [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-nettoala-kertoimella]}
        "D21" {:path [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine]}
        "E21" {:path [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-kerroin]}
        "F21" {:path [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-kertoimella]}
        "G21" {:path [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-nettoala-kertoimella]}
        "D22" {:path [:tulokset :kaytettavat-energiamuodot :summa]}
        "F22" {:path [:tulokset :kaytettavat-energiamuodot :kertoimella-summa]}
        "G22" {:path [:tulokset :kaytettavat-energiamuodot :nettoala-kertoimella-summa]}

        "E28" {:path [:tulokset :uusiutuvat-omavaraisenergiat :aurinkosahko]}
        "F28" {:path [:tulokset :uusiutuvat-omavaraisenergiat :aurinkosahko-nettoala]}
        "E29" {:path [:tulokset :uusiutuvat-omavaraisenergiat :aurinkolampo]}
        "F29" {:path [:tulokset :uusiutuvat-omavaraisenergiat :aurinkolampo-nettoala]}
        "E30" {:path [:tulokset :uusiutuvat-omavaraisenergiat :tuulisahko]}
        "F30" {:path [:tulokset :uusiutuvat-omavaraisenergiat :tuulisahko-nettoala]}
        "E31" {:path [:tulokset :uusiutuvat-omavaraisenergiat :lampopumppu]}
        "F31" {:path [:tulokset :uusiutuvat-omavaraisenergiat :lampopumppu-nettoala]}
        "E32" {:path [:tulokset :uusiutuvat-omavaraisenergiat :muusahko]}
        "F32" {:path [:tulokset :uusiutuvat-omavaraisenergiat :muusahko-nettoala]}
        "E33" {:path [:tulokset :uusiutuvat-omavaraisenergiat :muulampo]}
        "F33" {:path [:tulokset :uusiutuvat-omavaraisenergiat :muulampo-nettoala]}

        "E41" {:path [:tulokset :tekniset-jarjestelmat :tilojen-lammitys :sahko]}
        "F41" {:path [:tulokset :tekniset-jarjestelmat :tilojen-lammitys :lampo]}
        "E42" {:path [:tulokset :tekniset-jarjestelmat :tuloilman-lammitys :sahko]}
        "F42" {:path [:tulokset :tekniset-jarjestelmat :tuloilman-lammitys :lampo]}
        "E43" {:path [:tulokset :tekniset-jarjestelmat :kayttoveden-valmistus :sahko]}
        "F43" {:path [:tulokset :tekniset-jarjestelmat :kayttoveden-valmistus :lampo]}
        "E44" {:path [:tulokset :tekniset-jarjestelmat :iv-sahko]}
        "E45" {:path [:tulokset :tekniset-jarjestelmat :jaahdytys :sahko]}
        "F45" {:path [:tulokset :tekniset-jarjestelmat :jaahdytys :lampo]}
        "G45" {:path [:tulokset :tekniset-jarjestelmat :jaahdytys :kaukojaahdytys]}
        "E46" {:path [:tulokset :tekniset-jarjestelmat :kuluttajalaitteet-ja-valaistus-sahko]}

        "E47" {:path [:tulokset :tekniset-jarjestelmat :sahko-summa]}
        "F47" {:path [:tulokset :tekniset-jarjestelmat :lampo-summa]}
        "G47" {:path [:tulokset :tekniset-jarjestelmat :kaukojaahdytys-summa]}

        "E54" {:path [:tulokset :nettotarve :tilojen-lammitys-vuosikulutus]}
        "F54" {:path [:tulokset :nettotarve :tilojen-lammitys-vuosikulutus-nettoala]}
        "E55" {:path [:tulokset :nettotarve :ilmanvaihdon-lammitys-vuosikulutus]}
        "F55" {:path [:tulokset :nettotarve :ilmanvaihdon-lammitys-vuosikulutus-nettoala]}
        "E56" {:path [:tulokset :nettotarve :kayttoveden-valmistus-vuosikulutus]}
        "F56" {:path [:tulokset :nettotarve :kayttoveden-valmistus-vuosikulutus-nettoala]}
        "E57" {:path [:tulokset :nettotarve :jaahdytys-vuosikulutus]}
        "F57" {:path [:tulokset :nettotarve :jaahdytys-vuosikulutus-nettoala]}

        "E66" {:path [:tulokset :lampokuormat :aurinko]}
        "F66" {:path [:tulokset :lampokuormat :aurinko-nettoala]}
        "E67" {:path [:tulokset :lampokuormat :ihmiset]}
        "F67" {:path [:tulokset :lampokuormat :ihmiset-nettoala]}
        "E68" {:path [:tulokset :lampokuormat :kuluttajalaitteet]}
        "F68" {:path [:tulokset :lampokuormat :kuluttajalaitteet-nettoala]}
        "E69" {:path [:tulokset :lampokuormat :valaistus]}
        "F69" {:path [:tulokset :lampokuormat :valaistus-nettoala]}
        "E70" {:path [:tulokset :lampokuormat :kvesi]}
        "F70" {:path [:tulokset :lampokuormat :kvesi-nettoala]}

        "E74" {:path [:tulokset :laskentatyokalu]}}
     4 {"C7" #(format "Lämmitetty nettoala %s m²" (-> % :lahtotiedot :lammitetty-nettoala))

        "H12" {:path [:toteutunut-ostoenergiankulutus :ostettu-energia :kaukolampo-vuosikulutus]}
        "I12" {:path [:toteutunut-ostoenergiankulutus :ostettu-energia :kaukolampo-vuosikulutus-nettoala]}
        "H14" {:path [:toteutunut-ostoenergiankulutus :ostettu-energia :kokonaissahko-vuosikulutus]}
        "I14" {:path [:toteutunut-ostoenergiankulutus :ostettu-energia :kokonaissahko-vuosikulutus-nettoala]}
        "H16" {:path [:toteutunut-ostoenergiankulutus :ostettu-energia :kiinteistosahko-vuosikulutus]}
        "I16" {:path [:toteutunut-ostoenergiankulutus :ostettu-energia :kiinteistosahko-vuosikulutus-nettoala]}
        "H17" {:path [:toteutunut-ostoenergiankulutus :ostettu-energia :kayttajasahko-vuosikulutus]}
        "I17" {:path [:toteutunut-ostoenergiankulutus :ostettu-energia :kayttajasahko-vuosikulutus-nettoala]}
        "H19" {:path [:toteutunut-ostoenergiankulutus :ostettu-energia :kaukojaahdytys-vuosikulutus]}
        "I19" {:path [:toteutunut-ostoenergiankulutus :ostettu-energia :kaukojaahdytys-vuosikulutus-nettoala]}

        "E23" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :kevyt-polttooljy]}
        "H23" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :kevyt-polttooljy-kwh]}
        "I23" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :kevyt-polttooljy-kwh-nettoala]}
        "E24" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-havu-sekapuu]}
        "H24" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-havu-sekapuu-kwh]}
        "I24" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-havu-sekapuu-kwh-nettoala]}
        "E25" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-koivu]}
        "H25" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-koivu-kwh]}
        "I25" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-koivu-kwh-nettoala]}
        "E26" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :puupelletit]}
        "H26" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :puupelletit-kwh]}
        "I26" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :puupelletit-kwh-nettoala]}

        "C27" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 0 :nimi]}
        "E27" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 0 :maara-vuodessa]}
        "F27" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 0 :yksikko]}
        "G27" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 0 :muunnoskerroin]}
        "H27" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 0 :kwh]}
        "I27" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 0 :kwh-nettoala]}
        "C28" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 1 :nimi]}
        "E28" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 1 :maara-vuodessa]}
        "F28" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 1 :yksikko]}
        "G28" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 1 :muunnoskerroin]}
        "H28" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 1 :kwh]}
        "I28" {:path [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 1 :kwh-nettoala]}

        "H38" {:path [:toteutunut-ostoenergiankulutus :sahko-vuosikulutus-yhteensa]}
        "I38" {:path [:toteutunut-ostoenergiankulutus :sahko-vuosikulutus-yhteensa-nettoala]}
        "H40" {:path [:toteutunut-ostoenergiankulutus :kaukolampo-vuosikulutus-yhteensa]}
        "I40" {:path [:toteutunut-ostoenergiankulutus :kaukolampo-vuosikulutus-yhteensa-nettoala]}
        "H42" {:path [:toteutunut-ostoenergiankulutus :polttoaineet-vuosikulutus-yhteensa]}
        "I42" {:path [:toteutunut-ostoenergiankulutus :polttoaineet-vuosikulutus-yhteensa-nettoala]}
        "H44" {:path [:toteutunut-ostoenergiankulutus :kaukojaahdytys-vuosikulutus-yhteensa]}
        "I44" {:path [:toteutunut-ostoenergiankulutus :kaukojaahdytys-vuosikulutus-yhteensa-nettoala]}
        "H46" {:path [:toteutunut-ostoenergiankulutus :summa]}
        "I46" {:path [:toteutunut-ostoenergiankulutus :summa-nettoala]}}
     5 {"B5" {:path [:huomiot :ymparys :teksti-fi]}
        "C12" {:path [:huomiot :ymparys :toimenpide 0 :nimi-fi]}
        "C13" {:path [:huomiot :ymparys :toimenpide 1 :nimi-fi]}
        "C14" {:path [:huomiot :ymparys :toimenpide 2 :nimi-fi]}
        "C17" {:path [:huomiot :ymparys :toimenpide 0 :lampo]}
        "D17" {:path [:huomiot :ymparys :toimenpide 0 :sahko]}
        "E17" {:path [:huomiot :ymparys :toimenpide 0 :jaahdytys]}
        "F17" {:path [:huomiot :ymparys :toimenpide 0 :eluvun-muutos]}
        "C18" {:path [:huomiot :ymparys :toimenpide 1 :lampo]}
        "D18" {:path [:huomiot :ymparys :toimenpide 1 :sahko]}
        "E18" {:path [:huomiot :ymparys :toimenpide 1 :jaahdytys]}
        "F18" {:path [:huomiot :ymparys :toimenpide 1 :eluvun-muutos]}
        "C19" {:path [:huomiot :ymparys :toimenpide 2 :lampo]}
        "D19" {:path [:huomiot :ymparys :toimenpide 2 :sahko]}
        "E19" {:path [:huomiot :ymparys :toimenpide 2 :jaahdytys]}
        "F19" {:path [:huomiot :ymparys :toimenpide 2 :eluvun-muutos]}

        "B21" {:path [:huomiot :alapohja-ylapohja :teksti-fi]}
        "C28" {:path [:huomiot :alapohja-ylapohja :toimenpide 0 :nimi-fi]}
        "C29" {:path [:huomiot :alapohja-ylapohja :toimenpide 1 :nimi-fi]}
        "C30" {:path [:huomiot :alapohja-ylapohja :toimenpide 2 :nimi-fi]}
        "C33" {:path [:huomiot :alapohja-ylapohja :toimenpide 0 :lampo]}
        "D33" {:path [:huomiot :alapohja-ylapohja :toimenpide 0 :sahko]}
        "E33" {:path [:huomiot :alapohja-ylapohja :toimenpide 0 :jaahdytys]}
        "F33" {:path [:huomiot :alapohja-ylapohja :toimenpide 0 :eluvun-muutos]}
        "C34" {:path [:huomiot :alapohja-ylapohja :toimenpide 1 :lampo]}
        "D34" {:path [:huomiot :alapohja-ylapohja :toimenpide 1 :sahko]}
        "E34" {:path [:huomiot :alapohja-ylapohja :toimenpide 1 :jaahdytys]}
        "F34" {:path [:huomiot :alapohja-ylapohja :toimenpide 1 :eluvun-muutos]}
        "C35" {:path [:huomiot :alapohja-ylapohja :toimenpide 2 :lampo]}
        "D35" {:path [:huomiot :alapohja-ylapohja :toimenpide 2 :sahko]}
        "E35" {:path [:huomiot :alapohja-ylapohja :toimenpide 2 :jaahdytys]}
        "F35" {:path [:huomiot :alapohja-ylapohja :toimenpide 2 :eluvun-muutos]}

        "B37" {:path [:huomiot :lammitys :teksti-fi]}
        "C44" {:path [:huomiot :lammitys :toimenpide 0 :nimi-fi]}
        "C45" {:path [:huomiot :lammitys :toimenpide 1 :nimi-fi]}
        "C46" {:path [:huomiot :lammitys :toimenpide 2 :nimi-fi]}
        "C49" {:path [:huomiot :lammitys :toimenpide 0 :lampo]}
        "D49" {:path [:huomiot :lammitys :toimenpide 0 :sahko]}
        "E49" {:path [:huomiot :lammitys :toimenpide 0 :jaahdytys]}
        "F49" {:path [:huomiot :lammitys :toimenpide 0 :eluvun-muutos]}
        "C50" {:path [:huomiot :lammitys :toimenpide 1 :lampo]}
        "D50" {:path [:huomiot :lammitys :toimenpide 1 :sahko]}
        "E50" {:path [:huomiot :lammitys :toimenpide 1 :jaahdytys]}
        "F50" {:path [:huomiot :lammitys :toimenpide 1 :eluvun-muutos]}
        "C51" {:path [:huomiot :lammitys :toimenpide 2 :lampo]}
        "D51" {:path [:huomiot :lammitys :toimenpide 2 :sahko]}
        "E51" {:path [:huomiot :lammitys :toimenpide 2 :jaahdytys]}
        "F51" {:path [:huomiot :lammitys :toimenpide 2 :eluvun-muutos]}}
     6 {"B3" {:path [:huomiot :iv-ilmastointi :teksti-fi]}
        "C11" {:path [:huomiot :iv-ilmastointi :toimenpide 0 :nimi-fi]}
        "C12" {:path [:huomiot :iv-ilmastointi :toimenpide 1 :nimi-fi]}
        "C13" {:path [:huomiot :iv-ilmastointi :toimenpide 2 :nimi-fi]}
        "C16" {:path [:huomiot :iv-ilmastointi :toimenpide 0 :lampo]}
        "D16" {:path [:huomiot :iv-ilmastointi :toimenpide 0 :sahko]}
        "E16" {:path [:huomiot :iv-ilmastointi :toimenpide 0 :jaahdytys]}
        "F16" {:path [:huomiot :iv-ilmastointi :toimenpide 0 :eluvun-muutos]}
        "C17" {:path [:huomiot :iv-ilmastointi :toimenpide 1 :lampo]}
        "D17" {:path [:huomiot :iv-ilmastointi :toimenpide 1 :sahko]}
        "E17" {:path [:huomiot :iv-ilmastointi :toimenpide 1 :jaahdytys]}
        "F17" {:path [:huomiot :iv-ilmastointi :toimenpide 1 :eluvun-muutos]}
        "C18" {:path [:huomiot :iv-ilmastointi :toimenpide 2 :lampo]}
        "D18" {:path [:huomiot :iv-ilmastointi :toimenpide 2 :sahko]}
        "E18" {:path [:huomiot :iv-ilmastointi :toimenpide 2 :jaahdytys]}
        "F18" {:path [:huomiot :iv-ilmastointi :toimenpide 2 :eluvun-muutos]}

        "B20" {:path [:huomiot :valaistus-muut :teksti-fi]}
        "C28" {:path [:huomiot :valaistus-muut :toimenpide 0 :nimi-fi]}
        "C29" {:path [:huomiot :valaistus-muut :toimenpide 1 :nimi-fi]}
        "C30" {:path [:huomiot :valaistus-muut :toimenpide 2 :nimi-fi]}
        "C33" {:path [:huomiot :valaistus-muut :toimenpide 0 :lampo]}
        "D33" {:path [:huomiot :valaistus-muut :toimenpide 0 :sahko]}
        "E33" {:path [:huomiot :valaistus-muut :toimenpide 0 :jaahdytys]}
        "F33" {:path [:huomiot :valaistus-muut :toimenpide 0 :eluvun-muutos]}
        "C34" {:path [:huomiot :valaistus-muut :toimenpide 1 :lampo]}
        "D34" {:path [:huomiot :valaistus-muut :toimenpide 1 :sahko]}
        "E34" {:path [:huomiot :valaistus-muut :toimenpide 1 :jaahdytys]}
        "F34" {:path [:huomiot :valaistus-muut :toimenpide 1 :eluvun-muutos]}
        "C35" {:path [:huomiot :valaistus-muut :toimenpide 2 :lampo]}
        "D35" {:path [:huomiot :valaistus-muut :toimenpide 2 :sahko]}
        "E35" {:path [:huomiot :valaistus-muut :toimenpide 2 :jaahdytys]}
        "F35" {:path [:huomiot :valaistus-muut :toimenpide 2 :eluvun-muutos]}

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
        (doseq [[cell map-or-f] sheet-mappings
                :when map-or-f]
          (xlsx/set-cell-value-at (nth sheets sheet)
                                  cell
                                  (if (map? map-or-f)
                                    (get-in complete-energiatodistus (:path map-or-f))
                                    (map-or-f complete-energiatodistus)))))
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
