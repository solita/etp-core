(ns solita.etp.schema.energiatodistus
  (:require [solita.common.map :as m]
            [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.geo :as geo-schema]))

(defn optional-properties [schema]
  (m/map-values
    #(cond
       (instance? schema.core.Maybe %) %
       (instance? schema.core.Constrained %) (schema/maybe %)
       (instance? schema.core.EnumSchema %) (schema/maybe %)
       (class? %) (schema/maybe %)
       (map? %) (optional-properties %)
       (vector? %) (mapv optional-properties %)
       (coll? %) (map optional-properties %))
    schema))

(def Kayttotarkoitus
  (schema/enum "T" "TG" "UH" "TE" "MYR" "TT" "RT" "MU" "KK" "AK3" "YAT" "TOKK" "JH" "PK" "PT" "MR" "S" "PTK" "SR"
               "H" "KAT" "E" "AR" "KREP" "KIR" "LR" "MRVR" "V" "HL" "N" "A" "MH" "OR" "LH" "MLR" "AK2"))

(def Rakennustunnus
  (schema/constrained schema/Str #(= (count %) 10)))

(def YritysPostinumero common-schema/String8)

(def Yritys
  {:nimi             common-schema/String150
   :katuosoite       common-schema/String100
   :postinumero      YritysPostinumero
   :postitoimipaikka common-schema/String30})

(def Perustiedot
  {:katuosoite-fi            common-schema/String100
   :katuosoite-sv            common-schema/String100
   :valmistumisvuosi         common-schema/Year
   :onko-julkinen-rakennus   schema/Bool
   :havainnointikaynti       common-schema/Date
   :rakennustunnus           Rakennustunnus
   :postinumero              geo-schema/Postinumero
   :keskeiset-suositukset-fi common-schema/String2500
   :keskeiset-suositukset-sv common-schema/String2500
   :laatimisvaihe            (schema/enum 0 1 2)
   :kiinteistotunnus         common-schema/String50
   :yritys                   Yritys
   :tilaaja                  common-schema/String200
   :rakennusosa              common-schema/String100
   :kieli                    (schema/enum 0 1 2)
   :nimi                     common-schema/String50
   :kayttotarkoitus          Kayttotarkoitus})

(defn Rakenneusvaippa [mininclusive maxinclusive]
  {:ala common-schema/FloatPos
   :U (common-schema/FloatBase mininclusive maxinclusive)})

(def LahtotiedotRakennusvaippa
  {:ilmanvuotoluku common-schema/Float50
   :ulkoseinat     (Rakenneusvaippa 0.05 2.0)
   :ylapohja       (Rakenneusvaippa 0.03 2.0)
   :alapohja       (Rakenneusvaippa 0.03 4.0)
   :ikkunat        (Rakenneusvaippa 0.04 6.5)
   :ulkoovet       (Rakenneusvaippa 0.2 6.5)
   :kylmasillat-UA common-schema/FloatPos})

(def LahtotiedotIkkuna
  {:ala common-schema/FloatPos
   :U (common-schema/FloatBase 0.4 6.5)
   :g-ks (common-schema/FloatBase 0.1 1.0)})

(def LahtotiedotIkkunat
  {:pohjoinen LahtotiedotIkkuna
   :koillinen LahtotiedotIkkuna
   :ita       LahtotiedotIkkuna
   :kaakko    LahtotiedotIkkuna
   :etela     LahtotiedotIkkuna
   :lounas    LahtotiedotIkkuna
   :lansi     LahtotiedotIkkuna
   :luode     LahtotiedotIkkuna})

(def PoistoTuloSfp
  {:poisto common-schema/FloatPos
   :tulo   common-schema/FloatPos
   :sfp    common-schema/Float10})

(def LahtotiedotIlmanvaihto
  {:erillispoistot      PoistoTuloSfp
   :ivjarjestelma       PoistoTuloSfp
   :kuvaus-fi           common-schema/String75
   :kuvaus-sv           common-schema/String75
   :lto-vuosihyotysuhde common-schema/Float1
   :paaiv               (merge PoistoTuloSfp {:lampotilasuhde common-schema/Float1
                                              :jaatymisenesto (common-schema/FloatBase -20.0 10.0)})})

(def Hyotysuhde
  {:tuoton-hyotysuhde common-schema/FloatPos,
   :jaon-hyotysuhde   common-schema/FloatPos,
   :lampokerroin      common-schema/FloatPos,
   :apulaitteet       common-schema/FloatPos})


(def MaaraTuotto
  {:maara  common-schema/Integer100,
   :tuotto common-schema/FloatPos})

(def LahtotiedotLammitys
  {:kuvaus-fi         common-schema/String75
   :kuvaus-sv         common-schema/String75
   :tilat-ja-iv       Hyotysuhde
   :lammin-kayttovesi Hyotysuhde
   :takka             MaaraTuotto
   :ilmanlampopumppu  MaaraTuotto})

(def SisKuorma
  {:selite-fi         common-schema/String35
   :selite-sv         common-schema/String35
   :kayttoaste        (common-schema/FloatBase 0.1 1.0)
   :henkilot          (common-schema/FloatBase 1.0 14.0)
   :kuluttajalaitteet (common-schema/FloatBase 0.0 12.0)
   :valaistus         (common-schema/FloatBase 0.0 19.0)})

(def Lahtotiedot
  {:lammitetty-nettoala  common-schema/FloatPos
   :rakennusvaippa       LahtotiedotRakennusvaippa
   :ikkunat              LahtotiedotIkkunat
   :ilmanvaihto          LahtotiedotIlmanvaihto
   :lammitys             LahtotiedotLammitys
   :jaahdytysjarjestelma {:jaahdytyskauden-painotettu-kylmakerroin (common-schema/FloatBase 1.0 10.0)}
   :lkvn-kaytto          {:kulutus-per-nelio common-schema/FloatPos
                          :vuosikulutus      common-schema/FloatPos}
   :sis-kuorma           [SisKuorma]})

(def SahkoLampo
  {:sahko common-schema/FloatPos
   :lampo common-schema/FloatPos})

(def Tulokset
  {:kaytettavat-energiamuodot    [{:vakio                (schema/enum "fossiilinen polttoaine"
                                                                      "sähkö"
                                                                      "kaukojäähdytys"
                                                                      "kaukolämpö"
                                                                      "uusiutuva polttoaine"),
                                   :laskettu-ostoenergia schema/Num}],
   :uusiutuvat-omavaraisenergiat [{:nimi-vakio   (schema/enum "aurinkosahko"
                                                              "tuulisahko"
                                                              "aurinkolampo"
                                                              "muulampo"
                                                              "muusahko"
                                                              "lampopumppu"),
                                   :vuosikulutus common-schema/FloatPos}]
   :tekniset-jarjestelmat        {:tilojen-lammitys                     SahkoLampo,
                                  :tuloilman-lammitys                   SahkoLampo,
                                  :kayttoveden-valmistus                common-schema/FloatPos
                                  :iv-sahko                             common-schema/FloatPos
                                  :jaahdytys                            common-schema/FloatPos
                                  :kuluttajalaitteet-ja-valaistus-sahko common-schema/FloatPos},
   :nettotarve                   {:tilojen-lammitys-vuosikulutus      common-schema/FloatPos
                                  :ilmanvaihdon-lammitys-vuosikulutus common-schema/FloatPos
                                  :kayttoveden-valmistus-vuosikulutus common-schema/FloatPos
                                  :jaahdytys-vuosikulutus             common-schema/FloatPos},
   :lampokuormat                 {:aurinko           common-schema/FloatPos
                                  :ihmiset           common-schema/FloatPos
                                  :kuluttajalaitteet common-schema/FloatPos
                                  :valaistus         common-schema/FloatPos,
                                  :kvesi             common-schema/FloatPos},
   :laskentatyokalu              common-schema/String60})

(def ToteutunutOstoenergiankulutus
  {:ostettu-energia                         {:kaukolampo-vuosikulutus      common-schema/FloatPos,
                                             :kokonaissahko-vuosikulutus   common-schema/FloatPos,
                                             :kiinteistosahko-vuosikulutus common-schema/FloatPos,
                                             :kayttajasahko-vuosikulutus   common-schema/FloatPos,
                                             :kaukojaahdytys-vuosikulutus  common-schema/FloatPos},
   :ostetut-polttoaineet                    {:kevyt-polttooljy      common-schema/FloatPos,
                                             :pilkkeet-havu-sekapuu common-schema/FloatPos,
                                             :pilkkeet-koivu        common-schema/FloatPos,
                                             :puupelletit           common-schema/FloatPos,
                                             :vapaa                 [{:nimi           common-schema/String30,
                                                                      :yksikko        common-schema/String12,
                                                                      :muunnoskerroin common-schema/FloatPos,
                                                                      :maara-vuodessa common-schema/FloatPos}]},
   :sahko-vuosikulutus-yhteensa          common-schema/FloatPos,
   :kaukolampo-vuosikulutus-yhteensa     common-schema/FloatPos,
   :polttoaineet-vuosikulutus-yhteensa   common-schema/FloatPos,
   :kaukojaahdytys-vuosikulutus-yhteensa common-schema/FloatPos})

(def Huomio
  {:teksti-fi  common-schema/String1000
   :teksti-sv  common-schema/String1000,
   :toimenpide [{:nimi-fi       common-schema/String100
                 :nimi-sv       common-schema/String100
                 :lampo         common-schema/FloatPos,
                 :sahko         common-schema/FloatPos,
                 :jaahdytys     common-schema/FloatPos,
                 :eluvun-muutos common-schema/FloatPos}]})

(def Huomiot
  {:suositukset-fi    common-schema/String1500
   :suositukset-sv    common-schema/String1500
   :lisatietoja-fi    common-schema/String500
   :lisatietoja-sv    common-schema/String500
   :iv-ilmastointi    Huomio
   :valaistus-muut    Huomio
   :lammitys          Huomio
   :ymparys           Huomio
   :alapohja-ylapohja Huomio})

(def EnergiatodistusSave2018
  "This schema is used in add-energiatodistus and update-energiatodistus services"
  (optional-properties
    {:perustiedot                    Perustiedot
     :lahtotiedot                    Lahtotiedot
     :tulokset                       Tulokset
     :toteutunut-ostoenergiankulutus ToteutunutOstoenergiankulutus
     :huomiot                        Huomiot
     :lisamerkintoja-fi              common-schema/String6300
     :lisamerkintoja-sv              common-schema/String6300}))

(def EnergiatodistusTila
  {:tila (schema/enum "luonnos" "valmis")})

(def Energiatodistus2018
  "Energiatodistus schema contains basic information about persistent energiatodistus"
  (merge common-schema/Id EnergiatodistusTila EnergiatodistusSave2018))

(def Alakayttotarkoitusluokka
  (assoc common-schema/Luokittelu
    :kayttotarkoitusluokka-id common-schema/Key
    :id schema/Str))