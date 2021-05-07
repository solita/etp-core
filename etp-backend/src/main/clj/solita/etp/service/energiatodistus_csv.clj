(ns solita.etp.service.energiatodistus-csv
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [solita.etp.service.energiatodistus-search :as
             energiatodistus-search-service]
            [solita.etp.service.complete-energiatodistus
             :as complete-energiatodistus-service]))

(def tmp-dir "tmp-csv/")

(def columns
  (concat
   (for [k [:id :versio :tila-id :laatija-id :laatija-fullname
            :allekirjoitusaika :voimassaolo-paattymisaika
            :laskutusaika :draft-visible-to-paakayttaja :bypass-validation-limits
            :bypass-validation-limits-reason :korvattu-energiatodistus-id
            :korvaava-energiatodistus-id ;; TODO is this valid?
            :laskutettava-yritys-id :laskuriviviite]]
     [k])
   (for [child [:nimi :katuosoite :postinumero :postitoimipaikka]]
     [:perustiedot :yritys child])
   (for [child [:tilaaja :kieli :kieli-fi :kieli-sv :laatimisvaihe
                :laatimisvaihe-fi :laatimisvaihe-sv :havainnointikaynti
                :uudisrakennus]]
     [:perustiedot child])
   [[:tulokset :laskentatyokalu]]
   (for [child [:nimi :valmistumisvuosi :rakennusosa :katuosoite-fi
                :katuosoite-sv :postinumero :postitoimipaikka-fi
                :postitoimipaikka-sv :rakennustunnus :kiinteistotunnus
                :kayttotarkoitus :alakayttotarkoitus-fi :alakayttotarkoitus-sv
                :julkinen-rakennus]]
     [:perustiedot child])
   [[:tulokset :e-luku]
    [:tulokset :e-luokka]]
   (for [child [:keskeiset-suositukset-fi :keskeiset-suositukset-sv]]
     [:perustiedot child])
   [[:lahtotiedot :lammitetty-nettoala]]
   (for [child [:ilmanvuotoluku :lampokapasiteetti :ilmatilavuus]]
     [:lahtotiedot :rakennusvaippa child])
   (for [parent [:ulkoseinat :ylapohja :alapohja :ikkunat :ulkoovet]
         child [:ala :U :UA :osuus-lampohaviosta]]
     [:lahtotiedot :rakennusvaippa parent child])
   [[:lahtotiedot :rakennusvaippa :kylmasillat-UA]
    [:lahtotiedot :rakennusvaippa :kylmasillat-osuus-lampohaviosta]
    [:lahtotiedot :rakennusvaippa :UA-summa]]
   (for [parent [:pohjoinen :koillinen :ita :kaakko :etela :lounas :lansi
                 :luode :valokupu :katto]
         child [:ala :U :g-ks]]
     [:lahtotiedot :ikkunat parent child])
   (for [child [:tyyppi-id :label-fi :label-sv :kuvaus-fi :kuvaus-sv]]
     [:lahtotiedot :ilmanvaihto child])
   (for [child [:tulo :poisto :tulo-poisto :sfp :lampotilasuhde :jaatymisenesto]]
     [:lahtotiedot :ilmanvaihto :paaiv child])
   (for [parent [:erillispoistot :ivjarjestelma]
         child [:tulo :poisto :tulo-poisto :sfp]]
     [:lahtotiedot :ilmanvaihto parent child])
   [[:lahtotiedot :ilmanvaihto :lto-vuosihyotysuhde]
    [:lahtotiedot :ilmanvaihto :tuloilma-lampotila]]
   (for [parent [:lammitysmuoto-1 :lammitysmuoto-2 :lammonjako]
         child [:id :kuvaus-fi :kuvaus-sv]]
     [:lahtotiedot :lammitys parent child])
   [[:lahtotiedot :lammitys :lammitysmuoto-label-fi]
    [:lahtotiedot :lammitys :lammitysmuoto-label-sv]
    [:lahtotiedot :lammitys :lammonjako-label-fi]
    [:lahtotiedot :lammitys :lammonjako-label-sv]]
   (for [parent [:tilat-ja-iv :lammin-kayttovesi]
         child [:tuoton-hyotysuhde :jaon-hyotysuhde :lampokerroin :apulaitteet
                :lampohavio-lammittamaton-tila :lampopumppu-tuotto-osuus]]
     [:lahtotiedot :lammitys parent child])
   (for [parent [:takka :ilmalampopumppu]
         child [:maara :tuotto]]
     [:lahtotiedot :lammitys parent child])
   [[:lahtotiedot :jaahdytysjarjestelma :jaahdytyskauden-painotettu-kylmakerroin]
    [:lahtotiedot :lkvn-kaytto :ominaiskulutus]
    [:lahtotiedot :lkvn-kaytto :lammitysenergian-nettotarve]]
   (for [parent [:henkilot :kuluttajalaitteet :valaistus]
         child [:kayttoaste :lampokuorma]]
     [:lahtotiedot :sis-kuorma parent child])
   (for [child [:kaukolampo :kaukolampo-nettoala :kaukolampo-kerroin
                :kaukolampo-kertoimella :kaukolampo-nettoala-kertoimella :sahko
                :sahko-nettoala :sahko-kerroin :sahko-kertoimella
                :sahko-nettoala-kertoimella :uusiutuva-polttoaine
                :uusiutuva-polttoaine-nettoala :uusiutuva-polttoaine-kerroin
                :uusiutuva-polttoaine-kertoimella
                :uusiutuva-polttoaine-nettoala-kertoimella
                :fossiilinen-polttoaine :fossiilinen-polttoaine-nettoala
                :fossiilinen-polttoaine-kerroin
                :fossiilinen-polttoaine-kertoimella
                :fossiilinen-polttoaine-nettoala-kertoimella :kaukojaahdytys
                :kaukojaahdytys-nettoala :kaukojaahdytys-kerroin
                :kaukojaahdytys-kertoimella
                :kaukojaahdytys-nettoala-kertoimella
                :valaistus-kuluttaja-sahko :valaistus-kuluttaja-sahko-nettoala]]
     [:tulokset :kaytettavat-energiamuodot child])
   (for [child [:nimi :muotokerroin :ostoenergia :ostoenergia-nettoala
                :ostoenergia-kertoimella :ostoenergia-nettoala-kertoimella]]
     [:tulokset :kaytettavat-energiamuodot :muu 0 child])
   [[:tulokset :kaytettavat-energiamuodot :summa]
    [:tulokset :kaytettavat-energiamuodot :kertoimella-summa]]
   (for [child [:aurinkosahko :aurinkosahko-nettoala :aurinkolampo
                :aurinkolampo-nettoala :tuulisahko :tuulisahko-nettoala
                :lampopumppu :lampopumppu-nettoala :muusahko :muusahko-nettoala
                :muulampo :muulampo-nettoala]]
     [:tulokset :uusiutuvat-omavaraisenergiat child])
   (for [idx (range 6)
         child [:nimi-fi :nimi-sv :vuosikulutus :vuosikulutus-nettoala]]
     [:tulokset :uusiutuvat-omavaraisenergiat idx child])
   (for [parent [:tilojen-lammitys :tuloilman-lammitys :kayttoveden-valmistus]
         child [:sahko :lampo]]
     [:tulokset :tekniset-jarjestelmat parent child])
   [[:tulokset :tekniset-jarjestelmat :iv-sahko]]
   (for [child [:sahko :lampo :kaukojaahdytys]]
     [:tulokset :tekniset-jarjestelmat :jaahdytys child])
   [[:tulokset :tekniset-jarjestelmat :kuluttajalaitteet-ja-valaistus-sahko]
    [:tulokset :tekniset-jarjestelmat :sahko-summa]
    [:tulokset :tekniset-jarjestelmat :lampo-summa]
    [:tulokset :tekniset-jarjestelmat :kaukojaahdytys-summa]]
   (for [child [:tilojen-lammitys-vuosikulutus
                :tilojen-lammitys-vuosikulutus-nettoala
                :ilmanvaihdon-lammitys-vuosikulutus
                :ilmanvaihdon-lammitys-vuosikulutus-nettoala
                :kayttoveden-valmistus-vuosikulutus
                :kayttoveden-valmistus-vuosikulutus-nettoala
                :jaahdytys-vuosikulutus :jaahdytys-vuosikulutus-nettoala]]
     [:tulokset :nettotarve child])
   (for [child [:aurinko :aurinko-nettoala :ihmiset :ihmiset-nettoala
                :kuluttajalaitteet :kuluttajalaitteet-nettoala
                :valaistus :valaistus-nettoala :kvesi :kvesi-nettoala]]
     [:tulokset :lampokuormat child])
   (for [child [:kaukolampo-vuosikulutus :kaukolampo-vuosikulutus-nettoala
                :kokonaissahko-vuosikulutus :kokonaissahko-vuosikulutus-nettoala
                :kiinteistosahko-vuosikulutus
                :kiinteistosahko-vuosikulutus-nettoala
                :kayttajasahko-vuosikulutus :kayttajasahko-vuosikulutus-nettoala
                :kaukojaahdytys-vuosikulutus
                :kaukojaahdytys-vuosikulutus-nettoala]]
     [:toteutunut-ostoenergiankulutus :ostettu-energia child])
   (for [idx (range 5)
         child [:nimi-fi :nimi-sv :vuosikulutus :vuosikulutus-nettoala]]
     [:toteutunut-ostoenergiankulutus :ostettu-energia :muu idx child])
   (for [child [:kevyt-polttooljy :kevyt-polttooljy-kerroin
                :kevyt-polttooljy-kwh :kevyt-polttooljy-kwh-nettoala
                :pilkkeet-havu-sekapuu :pilkkeet-havu-sekapuu-kerroin
                :pilkkeet-havu-sekapuu-kwh :pilkkeet-havu-sekapuu-kwh-nettoala
                :pilkkeet-koivu :pilkkeet-koivu-kerroin :pilkkeet-koivu-kwh
                :pilkkeet-koivu-kwh-nettoala :puupelletit :puupelletit-kerroin
                :puupelletit-kwh :puupelletit-kwh-nettoala]]
     [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet child])
   (for [idx (range 3)
         child [:nimi :yksikko :muunnoskerroin :maara-vuodessa :kwh
                :kwn-nettoala]]
     [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :muu idx child])
   (for [child [:sahko-vuosikulutus-yhteensa
                :sahko-vuosikulutus-yhteensa-nettoala
                :kaukolampo-vuosikulutus-yhteensa
                :kaukolampo-vuosikulutus-yhteensa-nettoala
                :polttoaineet-vuosikulutus-yhteensa
                :polttoaineet-vuosikulutus-yhteensa-nettoala
                :kaukojaahdytys-vuosikulutus-yhteensa
                :kaukojaahdytys-vuosikulutus-yhteensa-nettoala
                :summa :summa-nettoala]]
     [:toteutunut-ostoenergiankulutus child])
   (for [child [:suositukset-fi :suositukset-sv :lisatietoja-fi
                :lisatietoja-sv]]
     [:huomiot child])
   (apply concat
          (for [parent [:iv-ilmastointi :valaistus-muut :lammitys :ymparys
                        :alapohja-ylapohja]]
            (concat
             [[:huomiot parent :teksti-fi]
              [:huomiot parent :teksti-sv]]
             (for [idx (range 3)
                   child [:nimi-fi :nimi-sv :lampo :sahko :jaahdytys
                          :eluvun-muutos]]
               [:huomiot parent :toimenpide idx child]))))
   [[:lisamerkintoja-fi]
    [:lisamerkintoja-sv]]))

(defn column-ks->str [ks]
  (->> ks
       (map #(if (keyword? %) (name %) %))
       (map str/capitalize)
       (str/join #" / ")))

(defn csv-line [coll]
  (as-> coll $
    (map #(if (string? %) (format "\"%s\"" %) %) $)
    (str/join #"," $)
    (str $ "\n")))

(defn write-to-csv! [path append? coll]
  (spit path (csv-line coll) :append :append?))

(defn create-csv! [path]
  (->> columns
       (map column-ks->str)
       (write-to-csv! path false)))

(defn append-energiatodistus-to-csv! [path energiatodistus]
  (->> columns
       (map #(get-in energiatodistus %))
       (write-to-csv! path true)))

(defn find-energiatodistukset-csv [db whoami query]
  (let [path (->> (java.util.UUID/randomUUID)
                  .toString
                  (format "%senergiatodistukset-%s.csv" tmp-dir))
        luokittelut (complete-energiatodistus-service/luokittelut db)]
    (io/make-parents path)
    (create-csv! path)
    (run! (comp #(append-energiatodistus-to-csv! path %)
             #(complete-energiatodistus-service/complete-energiatodistus
               %
               luokittelut)
             energiatodistus-search-service/db-row->energiatodistus)
          (energiatodistus-search-service/reducible-search db
                                                           whoami
                                                           query
                                                           false))
    (let [is (io/input-stream path)]
      (io/delete-file path)
      is)))