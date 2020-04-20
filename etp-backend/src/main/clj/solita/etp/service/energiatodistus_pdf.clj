(ns solita.etp.service.energiatodistus-pdf
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.tools.logging :as log]
            [solita.common.xlsx :as xlsx]))

(def xlsx-template-path "energiatodistus-template.xlsx")
(def sheet-count 8)
(def tmp-dir "tmp/")

(defn safe-str-* [x y]
  (when (and x y)
    (str (* x y))))

(defn safe-nth [coll idx]
  (when (> (count coll) idx)
    (nth coll idx)))

(defn kaytettavat-energiamuodot [energiatodistus]
  (let [{:keys [kaukolampo sahko fossiilinen-polttoaine
                kaukojaahdytys uusiutuva-polttoaine]}
        (-> energiatodistus :tulokset :kaytettavat-energiamuodot)]
    (->> [["Kaukolämpö" kaukolampo]
          ["Sähkö" sahko]
          ["Fossiilinen polttoaine" fossiilinen-polttoaine]
          ["Kaukojäähdytys" kaukojaahdytys]
          ["Uusiutuva polttoaine" uusiutuva-polttoaine]]
         (remove #(-> % second nil?))
         (into []))))

(def mappings {0 {"K7" [:perustiedot :nimi]
                  "K8" [:perustiedot :katuosoite-fi]

                  ;; TODO needs luokittelu for postitoimipaikka
                  "K9" #(str (-> % :perustiedot :postinumero) " " "Helsinki")
                  "K12" [:perustiedot :rakennustunnus]
                  "K13" [:perustiedot :valmistumisvuosi]

                  ;; TODO find alakayttotarkoitukset from db
                  "K14" [:perustiedot :kayttotarkoitus]
                  "K16" [:id]

                  ;; TODO checkboxes D19-D21
                  ;; TODO format date
                  "M21" [:perustiedot :havainnointikaynti]

                  ;; TODO M36 and M37 E-luku
                  ;; TODO laatija B42

                  "J42" [:perustiedot :yritys :nimi]

                  "B50" (fn [_] (str (java.time.LocalDate/now)))
                  "K50" (fn [_] (str (.plusYears (java.time.LocalDate/now) 10)))}
               1 {"F5" [:lahtotiedot :lammitetty-nettoala]
                  "F6" [:lahtotiedot :lammitys :kuvaus-fi]
                  "F7" [:lahtotiedot :ilmanvaihto :kuvaus-fi]

                  ;; TODO vakioduilla käytöllä lasketut ostoenergiat
                  ;; TODO Energiatehokkuuden vertailuluku

                  ;; TODO Käytetty E-luvun luokittelu asteikko

                  ;; TODO Luokkien rajat asteikolla

                  ;; TODO Tämän rakennuksen energiatehokkuusluokka

                  "C38" [:huomiot :suositukset-fi]}
               2 {
                  ;; TODO rakennuksen käyttötarkoitusluokka

                  ;; TODO Rakennusvaippa; osuus lämpöhäviöstä
                  "D5" [:perustiedot :valmistusmisvuosi]
                  "F5" [:lahtotiedot :lammitetty-nettoala]
                  "D7" [:lahtotiedot :rakennusvaippa :ilmanvaihtoluku]
                  "D10" [:lahtotiedot :rakennusvaippa :ulkoseinat :ala]
                  "E10" [:lahtotiedot :rakennusvaippa :ulkoseinat :U]
                  "F10" #(safe-str-* (-> % :lahtotiedot :rakennusvaippa :ulkoseinat :ala)
                                     (-> % :lahtotiedot :rakennusvaippa :ulkoseinat :U))
                  "D11" [:lahtotiedot :rakennusvaippa :ylapohja :ala]
                  "E11" [:lahtotiedot :rakennusvaippa :ylapohja :U]
                  "F11" #(safe-str-* (-> % :lahtotiedot :rakennusvaippa :ylapohja :ala)
                                     (-> % :lahtotiedot :rakennusvaippa :ylapohja :U))
                  "D12" [:lahtotiedot :rakennusvaippa :alapohja :ala]
                  "E12" [:lahtotiedot :rakennusvaippa :alapohja :U]
                  "F12" #(safe-str-* (-> % :lahtotiedot :rakennusvaippa :alapohja :ala)
                                     (-> % :lahtotiedot :rakennusvaippa :alapohja :U))
                  "D13" [:lahtotiedot :rakennusvaippa :ikkunat :ala]
                  "E13" [:lahtotiedot :rakennusvaippa :ikkunat :U]
                  "F13" #(safe-str-* (-> % :lahtotiedot :rakennusvaippa :ikkunat :ala)
                                     (-> % :lahtotiedot :rakennusvaippa :ikkunat :U))
                  "D14" [:lahtotiedot :rakennusvaippa :ulkoovet :ala]
                  "E14" [:lahtotiedot :rakennusvaippa :ulkoovet :U]
                  "F14" #(safe-str-* (-> % :lahtotiedot :rakennusvaippa :ulkoovet :ala)
                                     (-> % :lahtotiedot :rakennusvaippa :ulkoovet :U))
                  "F15" [:lahtotiedot :rakennusvaippa :kylmasillat-UA]

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

                  "D33" #(str (-> % :lahtotiedot :ilmanvaihto :paaiv :tulo)
                              " / "
                              (-> % :lahtotiedot :ilmanvaihto :paaiv :poisto))
                  "E33" [:lahtotiedot :ilmanvaihto :paaiv :sfp]
                  "F33" [:lahtotiedot :ilmanvaihto :paaiv :lampotilasuhde]
                  "G33" [:lahtotiedot :ilmanvaihto :paaiv :jaatymisenesto]
                  "D34" #(str (-> % :lahtotiedot :ilmanvaihto :erillispoistot :tulo)
                              " / "
                              (-> % :lahtotiedot :ilmanvaihto :erillispoistot :poisto))
                  "E34" [:lahtotiedot :ilmanvaihto :erillispoistot :sfp]
                  "D35" #(str (-> % :lahtotiedot :ilmanvaihto :ivjarjestelma :tulo)
                              " / "
                              (-> % :lahtotiedot :ilmanvaihto :ivjarjestelma :poisto))
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

                  "C63" [:lahtotiedot :sis-kuorma 0 :selite-fi]
                  "D63" [:lahtotiedot :sis-kuorma 0 :kayttoaste]
                  "E63" [:lahtotiedot :sis-kuorma 0 :henkilot]
                  "F63" [:lahtotiedot :sis-kuorma 0 :kuluttajalaitteet]
                  "G63" [:lahtotiedot :sis-kuorma 0 :valaistus]
                  "C64" [:lahtotiedot :sis-kuorma 1 :selite-fi]
                  "D64" [:lahtotiedot :sis-kuorma 1 :kayttoaste]
                  "E64" [:lahtotiedot :sis-kuorma 1 :henkilot]
                  "F64" [:lahtotiedot :sis-kuorma 1 :kuluttajalaitteet]
                  "G64" [:lahtotiedot :sis-kuorma 1 :valaistus]
                  "C65" [:lahtotiedot :sis-kuorma 2 :selite-fi]
                  "D65" [:lahtotiedot :sis-kuorma 2 :kayttoaste]
                  "E65" [:lahtotiedot :sis-kuorma 2 :henkilot]
                  "F65" [:lahtotiedot :sis-kuorma 2 :kuluttajalaitteet]
                  "G66" [:lahtotiedot :sis-kuorma 2 :valaistus]}
               3 {
                  ;; TODO rakennuksen käyttötarkoitusluokka

                  "D7" [:perustiedot :valmistumisvuosi]
                  "D8" [:lahtotiedot :lammitetty-nettoala]

                  ;; TODO e-luku

                  "C17" #(-> % kaytettavat-energiamuodot (safe-nth 0) first)
                  "D17" #(-> % kaytettavat-energiamuodot (safe-nth 0) second str)
                  "C18" #(-> % kaytettavat-energiamuodot (safe-nth 1) first)
                  "D18" #(-> % kaytettavat-energiamuodot (safe-nth 1) second str)
                  "C19" #(-> % kaytettavat-energiamuodot (safe-nth 2) first)
                  "D19" #(-> % kaytettavat-energiamuodot (safe-nth 2) second str)
                  "C20" #(-> % kaytettavat-energiamuodot (safe-nth 3) first)
                  "D20" #(-> % kaytettavat-energiamuodot (safe-nth 3) second str)
                  "C21" #(-> % kaytettavat-energiamuodot (safe-nth 4) first)
                  "D21" #(-> % kaytettavat-energiamuodot (safe-nth 4) second str)

                  ;; TODO energiamuodon kerroin ja energiamuodon
                  ;; kertoimella painotettu energiankulutus

                  "E28" [:tulokset :uusiutuvat-omavaraisenergiat :aurinkosahko]
                  "E29" [:tulokset :uusiutuvat-omavaraisenergiat :aurinkolampo]
                  "E30" [:tulokset :uusiutuvat-omavaraisenergiat :tuulisahko]
                  "E31" [:tulokset :uusiutuvat-omavaraisenergiat :lampopumppu]
                  "E32" [:tulokset :uusiutuvat-omavaraisenergiat :muusahko]
                  "E33" [:tulokset :uusiutuvat-omavaraisenergiat :muulampo]

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
                  "E55" [:tulokset :nettotarve :ilmanvaihdon-lammitys-vuosikulutus]
                  "E56" [:tulokset :nettotarve :kayttoveden-valmistus-vuosikulutus]
                  "E57" [:tulokset :nettotarve :jaahdytys-vuosikulutus]

                  "E66" [:tulokset :lampokuormat :aurinko]
                  "E67" [:tulokset :lampokuormat :ihmiset]
                  "E68" [:tulokset :lampokuormat :kuluttajalaitteet]
                  "E69" [:tulokset :lampokuormat :valaistus]
                  "E70" [:tulokset :lampokuormat :kvesi]

                  "E74" [:tulokset :laskentatyokalu]}

               })

(defn fill-xlsx-template [energiatodistus]
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
                                    (str (get-in energiatodistus cursor-or-f))
                                    (cursor-or-f energiatodistus)))))
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

(defn generate [energiatodistus]
  (let [xlsx-path (fill-xlsx-template energiatodistus)
        pdf-path (xlsx->pdf xlsx-path)
        is (io/input-stream pdf-path)]
    (io/delete-file xlsx-path)
    (io/delete-file pdf-path)
    is))
