(ns solita.etp.service.energiatodistus-pdf
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.tools.logging :as log]
            [puumerkki.pdf :as puumerkki]
            [solita.common.xlsx :as xlsx]
            [solita.etp.service.energiatodistus :as energiatodistus-service]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.service.file :as file-service])
  (:import (org.apache.pdfbox.tools OverlayPDF)
           (org.apache.pdfbox.multipdf Overlay$Position)))

(def xlsx-template-path "energiatodistus-template.xlsx")
(def watermark-path-fi "watermark-fi.pdf")
(def sheet-count 8)
(def tmp-dir "tmp/")

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

(def mappings {0 {"K7" [:perustiedot :nimi]
                  "K8" [:perustiedot :katuosoite-fi]

                  ;; TODO needs luokittelu for postitoimipaikka
                  "K9" #(str (-> % :perustiedot :postinumero) " " "Helsinki")
                  "K12" [:perustiedot :rakennustunnus]
                  "K13" [:perustiedot :valmistumisvuosi]

                  "K14" [:perustiedot :alakayttotarkoitus-fi]
                  "K16" [:id]

                  ;; TODO checkboxes D19-D21
                  ;; TODO format date
                  "M21" [:perustiedot :havainnointikaynti]

                  ;; TODO M36 and M37 E-luku
                  "B42" [:laatija-fullname]
                  "J42" [:perustiedot :yritys :nimi]

                  "B50" (fn [_] (str (java.time.LocalDate/now)))
                  "K50" (fn [_] (str (.plusYears (java.time.LocalDate/now) 10)))}
               1 {"F5" [:lahtotiedot :lammitetty-nettoala]
                  "F6" [:lahtotiedot :lammitys :kuvaus-fi]
                  "F7" [:lahtotiedot :ilmanvaihto :kuvaus-fi]
                  "F14" [:tulokset :kaytettavat-energiamuodot :kaukolampo]
                  "G14" [:tulokset :kaytettavat-energiamuodot :kaukolampo-nettoala]
                  "H14" [:tulokset :kaytettavat-energiamuodot :kaukolampo-kerroin]
                  "I14" [:tulokset :kaytettavat-energiamuodot :kaukolampo-nettoala-kertoimella]
                  "F15" [:tulokset :kaytettavat-energiamuodot :sahko]
                  "G15" [:tulokset :kaytettavat-energiamuodot :sahko-nettoala]
                  "H15" [:tulokset :kaytettavat-energiamuodot :sahko-kerroin]
                  "I15" [:tulokset :kaytettavat-energiamuodot :sahko-nettoala-kertoimella]
                  "F16" [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine]
                  "G16" [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-nettoala]
                  "H16" [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-kerroin]
                  "I16" [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-nettoala-kertoimella]
                  "F17" [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine]
                  "G17" [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-nettoala]
                  "H17" [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-kerroin]
                  "I17" [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-nettoala-kertoimella]
                  "F18" [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys]
                  "G18" [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-nettoala]
                  "H18" [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-kerroin]
                  "I18" [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-nettoala-kertoimella]

                  ;; TODO Energiatehokkuuden vertailuluku

                  ;; TODO Käytetty E-luvun luokittelu asteikko

                  ;; TODO Luokkien rajat asteikolla

                  ;; TODO Tämän rakennuksen energiatehokkuusluokka

                  "C38" [:perustiedot :keskeiset-suositukset-fi]}
               2 {"E4" [:perustiedot :alakayttotarkoitus-fi]
                  "D5" [:perustiedot :valmistusmisvuosi]
                  "F5" [:lahtotiedot :lammitetty-nettoala]
                  "D7" [:lahtotiedot :rakennusvaippa :ilmanvaihtoluku]

                  "D10" [:lahtotiedot :rakennusvaippa :ulkoseinat :ala]
                  "E10" [:lahtotiedot :rakennusvaippa :ulkoseinat :U]
                  "F10" [:lahtotiedot :rakennusvaippa :ulkoseinat :UA]
                  "G10" [:lahtotiedot :rakennusvaippa :ulkoseinat :osuus-lampohaviosta]
                  "D11" [:lahtotiedot :rakennusvaippa :ylapohja :ala]
                  "E11" [:lahtotiedot :rakennusvaippa :ylapohja :U]
                  "F11" [:lahtotiedot :rakennusvaippa :ylapohja :UA]
                  "G11" [:lahtotiedot :rakennusvaippa :ylapohja :osuus-lampohaviosta]
                  "D12" [:lahtotiedot :rakennusvaippa :alapohja :ala]
                  "E12" [:lahtotiedot :rakennusvaippa :alapohja :U]
                  "F12" [:lahtotiedot :rakennusvaippa :alapohja :UA]
                  "G12" [:lahtotiedot :rakennusvaippa :alapohja :osuus-lampohaviosta]
                  "D13" [:lahtotiedot :rakennusvaippa :ikkunat :ala]
                  "E13" [:lahtotiedot :rakennusvaippa :ikkunat :U]
                  "F13" [:lahtotiedot :rakennusvaippa :ikkunat :UA]
                  "G13" [:lahtotiedot :rakennusvaippa :ikkunat :osuus-lampohaviosta]
                  "D14" [:lahtotiedot :rakennusvaippa :ulkoovet :ala]
                  "E14" [:lahtotiedot :rakennusvaippa :ulkoovet :U]
                  "F14" [:lahtotiedot :rakennusvaippa :ulkoovet :UA]
                  "G14" [:lahtotiedot :rakennusvaippa :ulkoovet :osuus-lampohaviosta]
                  "F15" [:lahtotiedot :rakennusvaippa :kylmasillat-UA]
                  "G15" [:lahtotiedot :rakennusvaippa :kylmasillat-osuus-lampohaviosta]

                  "D19" [:lahtotiedot :ikkunat :pohjoinen :ala]
                  "E19" [:lahtotiedot :ikkunat :pohjoinen :U]
                  "F19" [:lahtotiedot :ikkunat :pohjoinen :g-ks]
                  "D20" [:lahtotiedot :ikkunat :koillinen :ala]
                  "E20" [:lahtotiedot :ikkunat :koillinen :U]
                  "F20" [:lahtotiedot :ikkunat :koillinen :g-ks]
                  "D21" [:lahtotiedot :ikkunat :ita :ala]
                  "E21" [:lahtotiedot :ikkunat :ita :U]
                  "F21" [:lahtotiedot :ikkunat :ita :g-ks]
                  "D22" [:lahtotiedot :ikkunat :kaakko :ala]
                  "E22" [:lahtotiedot :ikkunat :kaakko :U]
                  "F22" [:lahtotiedot :ikkunat :kaakko :g-ks]
                  "D23" [:lahtotiedot :ikkunat :etela :ala]
                  "E23" [:lahtotiedot :ikkunat :etela :U]
                  "F23" [:lahtotiedot :ikkunat :etela :g-ks]
                  "D24" [:lahtotiedot :ikkunat :lounas :ala]
                  "E24" [:lahtotiedot :ikkunat :lounas :U]
                  "F24" [:lahtotiedot :ikkunat :lounas :g-ks]
                  "D25" [:lahtotiedot :ikkunat :lansi :ala]
                  "E25" [:lahtotiedot :ikkunat :lansi :U]
                  "F25" [:lahtotiedot :ikkunat :lansi :g-ks]
                  "D26" [:lahtotiedot :ikkunat :luode :ala]
                  "E26" [:lahtotiedot :ikkunat :luode :U]
                  "F26" [:lahtotiedot :ikkunat :luode :g-ks]

                  "D28" [:lahtotiedot :ilmanvaihto :kuvaus-fi]
                  "D33" [:lahtotiedot :ilmanvaihto :paaiv :tulo-poisto]
                  "E33" [:lahtotiedot :ilmanvaihto :paaiv :sfp]
                  "F33" [:lahtotiedot :ilmanvaihto :paaiv :lampotilasuhde]
                  "G33" [:lahtotiedot :ilmanvaihto :paaiv :jaatymisenesto]
                  "D34" [:lahtotiedot :ilmanvaihto :erillispoistot :tulo-poisto]
                  "E34" [:lahtotiedot :ilmanvaihto :erillispoistot :sfp]
                  "D35" [:lahtotiedot :ilmanvaihto :ivjarjestelma :tulo-poisto]
                  "E35" [:lahtotiedot :ilmanvaihto :ivjarjestelma :sfp]
                  "E36" [:lahtotiedot :ilmanvaihto :lto-vuosihyotysuhde]

                  "D38" [:lahtotiedot :lammitys :kuvaus-fi]

                  "D43" [:lahtotiedot :lammitys :tilat-ja-iv :tuoton-hyotysuhde]
                  "E43" [:lahtotiedot :lammitys :tilat-ja-iv :jaon-hyotysuhde]
                  "F43" [:lahtotiedot :lammitys :tilat-ja-iv :lampokerroin]
                  "G43" [:lahtotiedot :lammitys :tilat-ja-iv :apulaitteet]
                  "D44" [:lahtotiedot :lammitys :lammin-kayttovesi :tuoton-hyotysuhde]
                  "E44" [:lahtotiedot :lammitys :lammin-kayttovesi :jaon-hyotysuhde]
                  "F44" [:lahtotiedot :lammitys :lammin-kayttovesi :lampokerroin]
                  "G44" [:lahtotiedot :lammitys :lammin-kayttovesi :apulaitteet]

                  "D50" [:lahtotiedot :lammitys :takka :maara]
                  "E50" [:lahtotiedot :lammitys :takka :tuotto]
                  "D51" [:lahtotiedot :lammitys :ilmanlampopumppu :maara]
                  "E51" [:lahtotiedot :lammitys :ilmanlampopumppu :tuotto]

                  "D55" [:lahtotiedot :jaahdytysjarjestelma :jaahdytyskauden-painotettu-kylmakerroin]

                  ;; TODO lämmin käyttövesi ominaiskulutus, lämmitysenergian nettotarve

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
               3 {"D4" [:perustiedot :alakayttotarkoitus-fi]
                  "D7" [:perustiedot :valmistumisvuosi]
                  "D8" [:lahtotiedot :lammitetty-nettoala]

                  ;; TODO e-luku

                  "D17" [:tulokset :kaytettavat-energiamuodot :kaukolampo]
                  "E17" [:tulokset :kaytettavat-energiamuodot :kaukolampo-kerroin]
                  "F17" [:tulokset :kaytettavat-energiamuodot :kaukolampo-kertoimella]
                  "G17" [:tulokset :kaytettavat-energiamuodot :kaukolampo-nettoala-kertoimella]
                  "D18" [:tulokset :kaytettavat-energiamuodot :sahko]
                  "E18" [:tulokset :kaytettavat-energiamuodot :sahko-kerroin]
                  "F18" [:tulokset :kaytettavat-energiamuodot :sahko-kertoimella]
                  "G18" [:tulokset :kaytettavat-energiamuodot :sahko-nettoala-kertoimella]
                  "D19" [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine]
                  "E19" [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-kerroin]
                  "F19" [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-kertoimella]
                  "G19" [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-nettoala-kertoimella]
                  "D20" [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys]
                  "E20" [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-kerroin]
                  "F20" [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-kertoimella]
                  "G20" [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-nettoala-kertoimella]
                  "D22" [:tulokset :kaytettavat-energiamuodot :summa-ilman-uusiutuvia]
                  "E22" [:tulokset :kaytettavat-energiamuodot :kerroin-summa-ilman-uusiutuvia]
                  "F22" [:tulokset :kaytettavat-energiamuodot :kertoimella-summa-ilman-uusiutuvia]
                  "G22" [:tulokset :kaytettavat-energiamuodot :nettoala-kertoimella-ilman-uusiutuvia]

                  "E28" [:tulokset :uusiutuvat-omavaraisenergiat :aurinkosahko]
                  "F28" [:tulokset :uusiutuvat-omavaraisenergiat :aurinkosahko-nettoala]
                  "E29" [:tulokset :uusiutuvat-omavaraisenergiat :aurinkolampo]
                  "F29" [:tulokset :uusiutuvat-omavaraisenergiat :aurinkolampo-nettoala]
                  "E30" [:tulokset :uusiutuvat-omavaraisenergiat :tuulisahko]
                  "F30" [:tulokset :uusiutuvat-omavaraisenergiat :tuulisahko-nettoala]
                  "E31" [:tulokset :uusiutuvat-omavaraisenergiat :lampopumppu]
                  "F31" [:tulokset :uusiutuvat-omavaraisenergiat :lampopumppu-nettoala]
                  "E32" [:tulokset :uusiutuvat-omavaraisenergiat :muusahko]
                  "F32" [:tulokset :uusiutuvat-omavaraisenergiat :muusahko-nettoala]
                  "E33" [:tulokset :uusiutuvat-omavaraisenergiat :muulampo]
                  "F33" [:tulokset :uusiutuvat-omavaraisenergiat :muulampo-nettoala]

                  "E41" [:tulokset :tekniset-jarjestelmat :tilojen-lammitys :sahko]
                  "F41" [:tulokset :tekniset-jarjestelmat :tilojen-lammitys :lampo]
                  "E42" [:tulokset :tekniset-jarjestelmat :tuloilman-lammitys :sahko]
                  "F42" [:tulokset :tekniset-jarjestelmat :tuloilman-lammitys :lampo]
                  "E43" [:tulokset :tekniset-jarjestelmat :kayttoveden-valmistus :sahko]
                  "F43" [:tulokset :tekniset-jarjestelmat :kayttoveden-valmistus :lampo]
                  "E44" [:tulokset :tekniset-jarjestelmat :iv-sahko]
                  "E45" [:tulokset :tekniset-jarjestelmat :jaahdytys :sahko]
                  "F45" [:tulokset :tekniset-jarjestelmat :jaahdytys :lampo]
                  "G45" [:tulokset :tekniset-jarjestelmat :jaahdytys :kaukojaahdytys]
                  "E46" [:tulokset :tekniset-jarjestelmat :kuluttajalaitteet-ja-valaistus-sahko]

                  "E54" [:tulokset :nettotarve :tilojen-lammitys-vuosikulutus]
                  "F54" [:tulokset :nettotarve :tilojen-lammitys-vuosikulutus-nettoala]
                  "E55" [:tulokset :nettotarve :ilmanvaihdon-lammitys-vuosikulutus]
                  "F55" [:tulokset :nettotarve :ilmanvaihdon-lammitys-vuosikulutus-nettoala]
                  "E56" [:tulokset :nettotarve :kayttoveden-valmistus-vuosikulutus]
                  "F56" [:tulokset :nettotarve :kayttoveden-valmistus-vuosikulutus-nettoala]
                  "E57" [:tulokset :nettotarve :jaahdytys-vuosikulutus]
                  "F57" [:tulokset :nettotarve :jaahdytys-vuosikulutus-nettoala]

                  "E66" [:tulokset :lampokuormat :aurinko]
                  "F66" [:tulokset :lampokuormat :aurinko-nettoala]
                  "E67" [:tulokset :lampokuormat :ihmiset]
                  "F67" [:tulokset :lampokuormat :ihmiset-nettoala]
                  "E68" [:tulokset :lampokuormat :kuluttajalaitteet]
                  "F68" [:tulokset :lampokuormat :kuluttajalaitteet-nettoala]
                  "E69" [:tulokset :lampokuormat :valaistus]
                  "F69" [:tulokset :lampokuormat :valaistus-nettoala]
                  "E70" [:tulokset :lampokuormat :kvesi]
                  "F70" [:tulokset :lampokuormat :kvesi-nettoala]

                  "E74" [:tulokset :laskentatyokalu]}
               4 {"C7" #(format "Lämmitetty nettoala %s m²"
                                (-> % :lahtotiedot :lammitetty-nettoala))

                  "H12" [:toteutunut-ostoenergiankulutus :ostettu-energia :kaukolampo-vuosikulutus]
                  "I12" [:toteutunut-ostoenergiankulutus :ostettu-energia :kaukolampo-vuosikulutus-nettoala]
                  "H14" [:toteutunut-ostoenergiankulutus :ostettu-energia :kokonaissahko-vuosikulutus]
                  "I14" [:toteutunut-ostoenergiankulutus :ostettu-energia :kokonaissahko-vuosikulutus-nettoala]
                  "H16" [:toteutunut-ostoenergiankulutus :ostettu-energia :kiinteistosahko-vuosikulutus]
                  "I16" [:toteutunut-ostoenergiankulutus :ostettu-energia :kiinteistosahko-vuosikulutus-nettoala]
                  "H17" [:toteutunut-ostoenergiankulutus :ostettu-energia :kayttajasahko-vuosikulutus]
                  "I17" [:toteutunut-ostoenergiankulutus :ostettu-energia :kayttajasahko-vuosikulutus-nettoala]
                  "H19" [:toteutunut-ostoenergiankulutus :ostettu-energia :kaukojaahdytys-vuosikulutus]
                  "I19" [:toteutunut-ostoenergiankulutus :ostettu-energia :kaukojaahdytys-vuosikulutus-nettoala]

                  "E23" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :kevyt-polttooljy]
                  "H23" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :kevyt-polttooljy-kwh]
                  "I23" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :kevyt-polttooljy-kwh-nettoala]
                  "E24" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-havu-sekapuu]
                  "H24" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-havu-sekapuu-kwh]
                  "I24" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-havu-sekapuu-kwh-nettoala]
                  "E25" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-koivu]
                  "H25" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-koivu-kwh]
                  "I25" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-koivu-kwh-nettoala]
                  "E26" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :puupelletit]
                  "H26" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :puupelletit-kwh]
                  "I26" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :puupelletit-kwh-nettoala]

                  "C27" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 0 :nimi]
                  "E27" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 0 :maara-vuodessa]
                  "F27" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 0 :yksikko]
                  "G27" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 0 :muunnoskerroin]
                  "H27" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 0 :kwh]
                  "I27" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 0 :kwh-nettoala]
                  "C28" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 1 :nimi]
                  "E28" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 1 :maara-vuodessa]
                  "F28" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 1 :yksikko]
                  "G28" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 1 :muunnoskerroin]
                  "H28" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 1 :kwh]
                  "I28" [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa 1 :kwh-nettoala]

                  "H38" [:toteutunut-ostoenergiankulutus :sahko-vuosikulutus-yhteensa]
                  "I38" [:toteutunut-ostoenergiankulutus :sahko-vuosikulutus-yhteensa-nettoala]
                  "H40" [:toteutunut-ostoenergiankulutus :kaukolampo-vuosikulutus-yhteensa]
                  "I40" [:toteutunut-ostoenergiankulutus :kaukolampo-vuosikulutus-yhteensa-nettoala]
                  "H42" [:toteutunut-ostoenergiankulutus :polttoaineet-vuosikulutus-yhteensa]
                  "I42" [:toteutunut-ostoenergiankulutus :polttoaineet-vuosikulutus-yhteensa-nettoala]
                  "H44" [:toteutunut-ostoenergiankulutus :kaukojaahdytys-vuosikulutus-yhteensa]
                  "I44" [:toteutunut-ostoenergiankulutus :kaukojaahdytys-vuosikulutus-yhteensa-nettoala]
                  "H46" [:toteutunut-ostoenergiankulutus :summa]
                  "I46" [:toteutunut-ostoenergiankulutus :summa-nettoala]}
               5 {"B5" [:huomiot :ymparys :teksti-fi]
                  "C12" [:huomiot :ymparys :toimenpide 0 :nimi-fi]
                  "C13" [:huomiot :ymparys :toimenpide 1 :nimi-fi]
                  "C14" [:huomiot :ymparys :toimenpide 2 :nimi-fi]
                  "C17" [:huomiot :ymparys :toimenpide 0 :lampo]
                  "D17" [:huomiot :ymparys :toimenpide 0 :sahko]
                  "E17" [:huomiot :ymparys :toimenpide 0 :jaahdytys]
                  "F17" [:huomiot :ymparys :toimenpide 0 :eluvun-muutos]
                  "C18" [:huomiot :ymparys :toimenpide 1 :lampo]
                  "D18" [:huomiot :ymparys :toimenpide 1 :sahko]
                  "E18" [:huomiot :ymparys :toimenpide 1 :jaahdytys]
                  "F18" [:huomiot :ymparys :toimenpide 1 :eluvun-muutos]
                  "C19" [:huomiot :ymparys :toimenpide 2 :lampo]
                  "D19" [:huomiot :ymparys :toimenpide 2 :sahko]
                  "E19" [:huomiot :ymparys :toimenpide 2 :jaahdytys]
                  "F19" [:huomiot :ymparys :toimenpide 2 :eluvun-muutos]

                  "B21" [:huomiot :alapohja-ylapohja :teksti-fi]
                  "C28" [:huomiot :alapohja-ylapohja :toimenpide 0 :nimi-fi]
                  "C29" [:huomiot :alapohja-ylapohja :toimenpide 1 :nimi-fi]
                  "C30" [:huomiot :alapohja-ylapohja :toimenpide 2 :nimi-fi]
                  "C33" [:huomiot :alapohja-ylapohja :toimenpide 0 :lampo]
                  "D33" [:huomiot :alapohja-ylapohja :toimenpide 0 :sahko]
                  "E33" [:huomiot :alapohja-ylapohja :toimenpide 0 :jaahdytys]
                  "F33" [:huomiot :alapohja-ylapohja :toimenpide 0 :eluvun-muutos]
                  "C34" [:huomiot :alapohja-ylapohja :toimenpide 1 :lampo]
                  "D34" [:huomiot :alapohja-ylapohja :toimenpide 1 :sahko]
                  "E34" [:huomiot :alapohja-ylapohja :toimenpide 1 :jaahdytys]
                  "F34" [:huomiot :alapohja-ylapohja :toimenpide 1 :eluvun-muutos]
                  "C35" [:huomiot :alapohja-ylapohja :toimenpide 2 :lampo]
                  "D35" [:huomiot :alapohja-ylapohja :toimenpide 2 :sahko]
                  "E35" [:huomiot :alapohja-ylapohja :toimenpide 2 :jaahdytys]
                  "F35" [:huomiot :alapohja-ylapohja :toimenpide 2 :eluvun-muutos]

                  "B37" [:huomiot :lammitys :teksti-fi]
                  "C44" [:huomiot :lammitys :toimenpide 0 :nimi-fi]
                  "C45" [:huomiot :lammitys :toimenpide 1 :nimi-fi]
                  "C46" [:huomiot :lammitys :toimenpide 2 :nimi-fi]
                  "C49" [:huomiot :lammitys :toimenpide 0 :lampo]
                  "D49" [:huomiot :lammitys :toimenpide 0 :sahko]
                  "E49" [:huomiot :lammitys :toimenpide 0 :jaahdytys]
                  "F49" [:huomiot :lammitys :toimenpide 0 :eluvun-muutos]
                  "C50" [:huomiot :lammitys :toimenpide 1 :lampo]
                  "D50" [:huomiot :lammitys :toimenpide 1 :sahko]
                  "E50" [:huomiot :lammitys :toimenpide 1 :jaahdytys]
                  "F50" [:huomiot :lammitys :toimenpide 1 :eluvun-muutos]
                  "C51" [:huomiot :lammitys :toimenpide 2 :lampo]
                  "D51" [:huomiot :lammitys :toimenpide 2 :sahko]
                  "E51" [:huomiot :lammitys :toimenpide 2 :jaahdytys]
                  "F51" [:huomiot :lammitys :toimenpide 2 :eluvun-muutos]}
               6 {"B3" [:huomiot :iv-ilmastointi :teksti-fi]
                  "C11" [:huomiot :iv-ilmastointi :toimenpide 0 :nimi-fi]
                  "C12" [:huomiot :iv-ilmastointi :toimenpide 1 :nimi-fi]
                  "C13" [:huomiot :iv-ilmastointi :toimenpide 2 :nimi-fi]
                  "C16" [:huomiot :iv-ilmastointi :toimenpide 0 :lampo]
                  "D16" [:huomiot :iv-ilmastointi :toimenpide 0 :sahko]
                  "E16" [:huomiot :iv-ilmastointi :toimenpide 0 :jaahdytys]
                  "F16" [:huomiot :iv-ilmastointi :toimenpide 0 :eluvun-muutos]
                  "C17" [:huomiot :iv-ilmastointi :toimenpide 1 :lampo]
                  "D17" [:huomiot :iv-ilmastointi :toimenpide 1 :sahko]
                  "E17" [:huomiot :iv-ilmastointi :toimenpide 1 :jaahdytys]
                  "F17" [:huomiot :iv-ilmastointi :toimenpide 1 :eluvun-muutos]
                  "C18" [:huomiot :iv-ilmastointi :toimenpide 2 :lampo]
                  "D18" [:huomiot :iv-ilmastointi :toimenpide 2 :sahko]
                  "E18" [:huomiot :iv-ilmastointi :toimenpide 2 :jaahdytys]
                  "F18" [:huomiot :iv-ilmastointi :toimenpide 2 :eluvun-muutos]

                  "B20" [:huomiot :valaistus-muut :teksti-fi]
                  "C28" [:huomiot :valaistus-muut :toimenpide 0 :nimi-fi]
                  "C29" [:huomiot :valaistus-muut :toimenpide 1 :nimi-fi]
                  "C30" [:huomiot :valaistus-muut :toimenpide 2 :nimi-fi]
                  "C33" [:huomiot :valaistus-muut :toimenpide 0 :lampo]
                  "D33" [:huomiot :valaistus-muut :toimenpide 0 :sahko]
                  "E33" [:huomiot :valaistus-muut :toimenpide 0 :jaahdytys]
                  "F33" [:huomiot :valaistus-muut :toimenpide 0 :eluvun-muutos]
                  "C34" [:huomiot :valaistus-muut :toimenpide 1 :lampo]
                  "D34" [:huomiot :valaistus-muut :toimenpide 1 :sahko]
                  "E34" [:huomiot :valaistus-muut :toimenpide 1 :jaahdytys]
                  "F34" [:huomiot :valaistus-muut :toimenpide 1 :eluvun-muutos]
                  "C35" [:huomiot :valaistus-muut :toimenpide 2 :lampo]
                  "D35" [:huomiot :valaistus-muut :toimenpide 2 :sahko]
                  "E35" [:huomiot :valaistus-muut :toimenpide 2 :jaahdytys]
                  "F35" [:huomiot :valaistus-muut :toimenpide 2 :eluvun-muutos]

                  "B37" [:huomiot :suositukset-fi]}
               7 {"B3" [:lisamerkintoja-fi]}})

(defn fill-xlsx-template [complete-energiatodistus]
  (with-open [is (-> xlsx-template-path io/resource io/input-stream)]
    (let [loaded-xlsx (xlsx/load-xlsx is)
          sheets (map #(xlsx/get-sheet loaded-xlsx %) (range sheet-count))
          path (->> (java.util.UUID/randomUUID)
                    .toString
                    (format "energiatodistus-%s.xlsx")
                    (str tmp-dir))]
      (doseq [[sheet sheet-mappings] mappings]
        (doseq [[cell cursor-or-f] sheet-mappings]
          (xlsx/set-cell-value-at (nth sheets sheet)
                                  cell
                                  (if (vector? cursor-or-f)
                                    (get-in complete-energiatodistus cursor-or-f)
                                    (cursor-or-f complete-energiatodistus)))))
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
        {:keys [exit err] :as sh-result} (shell/sh "libreoffice"
                                                   "--headless"
                                                   "--convert-to"
                                                   "pdf"
                                                   filename
                                                   :dir
                                                   dir)]
    (if (and (zero? exit) (str/blank? err))
      (str/replace path #".xlsx$" ".pdf")
      (do (log/error "XLSX to PDF conversion failed" sh-result)
        (throw (ex-info "XLSX to PDF conversion failed" sh-result))))))

(defn add-watermark [pdf-path]
  (let [settings ["-position" (.toString Overlay$Position/FOREGROUND)]
        watermark-path (-> watermark-path-fi io/resource .getPath)]
    (OverlayPDF/main (into-array String (flatten [pdf-path settings watermark-path pdf-path])))
    pdf-path))

(defn generate-pdf-as-file [complete-energiatodistus draft?]
  (let [xlsx-path (fill-xlsx-template complete-energiatodistus)
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

(defn do-when-signing [{:keys [allekirjoituksessaaika allekirjoitusaika]} f]
  (cond
    (nil? allekirjoituksessaaika)
    :not-in-signing

    (-> allekirjoitusaika nil? not)
    :already-signed

    :else
    (f)))

(defn find-energiatodistus-digest [db id]
  (when-let [{:keys [laatija-fullname] :as complete-energiatodistus}
             (energiatodistus-service/find-complete-energiatodistus db id)]
    (do-when-signing
     complete-energiatodistus
     #(let [pdf-path (generate-pdf-as-file complete-energiatodistus false)
            signable-pdf-path (puumerkki/add-signature-space
                               pdf-path
                               laatija-fullname)
            signable-pdf-data (puumerkki/read-file signable-pdf-path)
            digest (puumerkki/compute-base64-pkcs signable-pdf-data)
            file-id (pdf-file-id id)]
        (file-service/upsert-file-from-bytes db
                                             file-id
                                             (str file-id ".pdf")
                                             signable-pdf-data)
        (io/delete-file pdf-path)
        (io/delete-file signable-pdf-path)
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
