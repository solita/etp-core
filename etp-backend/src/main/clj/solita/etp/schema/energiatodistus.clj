(ns solita.etp.schema.energiatodistus
  (:require [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]))

(def Kayttotarkoitus
  (schema/enum "T" "TG" "UH" "TE" "MYR" "TT" "RT" "MU" "KK" "AK3" "YAT" "TOKK" "JH" "PK" "PT" "MR" "S" "PTK" "SR"
               "H" "KAT" "E" "AR" "KREP" "KIR" "LR" "MRVR" "V" "HL" "N" "A" "MH" "OR" "LH" "MLR" "AK2"))

(def Rakennustunnus
  (schema/constrained schema/Str #(= (count %) 10)))

(def Yritys
  {:nimi             common-schema/String150
   :katuosoite       (schema/maybe common-schema/String100)
   :postinumero      (schema/maybe common-schema/String8)
   :postitoimipaikka (schema/maybe common-schema/String30)})

(def Perustiedot
  {:katuosoite-fi            (schema/maybe common-schema/String100)
   :katuosoite-sv            (schema/maybe common-schema/String100)
   :valmistumisvuosi         common-schema/Year
   :onko-julkinen-rakennus   schema/Bool
   :havainnointikaynti       (schema/maybe common-schema/Date)
   :rakennustunnus           (schema/maybe Rakennustunnus)
   :postinumero              common-schema/Postinumero
   :keskeiset-suositukset-fi (schema/maybe common-schema/String2500)
   :keskeiset-suositukset-sv (schema/maybe common-schema/String2500)
   :laatimisvaihe            (schema/enum 0 1 2)
   :kiinteistotunnus         (schema/maybe common-schema/String50)
   :yritys                   Yritys
   :tilaaja                  (schema/maybe common-schema/String200)
   :rakennusosa              (schema/maybe common-schema/String100)
   :kieli                    (schema/maybe (schema/enum 0 1 2))
   :nimi                     (schema/maybe common-schema/String50)
   :kayttotarkoitus          Kayttotarkoitus})


(defn Rakenneusvaippa [mininclusive maxinclusive]
  {:rva common-schema/FloatPos
   :rvu (common-schema/FloatBase mininclusive maxinclusive)})

(def LahtotiedotRakenneusvaippa
  {:ilmanvuotoluku (schema/maybe common-schema/Float50)
   :ulkoseinat     (Rakenneusvaippa 0.05 2.0)
   :ylapohja       (Rakenneusvaippa 0.03 2.0)
   :alapohja       (Rakenneusvaippa 0.03 4.0)
   :ikkunat        (Rakenneusvaippa 0.04 6.5)
   :ulkoovet       (Rakenneusvaippa 0.2 6.5)
   :kylmasillat-UA (schema/maybe common-schema/FloatPos)})

(def LahtotiedotIkkuna
  {:ikkA common-schema/FloatPos
   :ikkU (common-schema/FloatBase 0.4 6.5)
   :ikkG (common-schema/FloatBase 0.1 1.0)})


(def LahtotiedotIkkunat
  {:pohjoinen LahtotiedotIkkuna
   :koillinen LahtotiedotIkkuna
   :ita       LahtotiedotIkkuna
   :kaakko    LahtotiedotIkkuna
   :etela     LahtotiedotIkkuna
   :lounas    LahtotiedotIkkuna
   :lansi     LahtotiedotIkkuna
   :luode     LahtotiedotIkkuna})

(def LahtotiedotIlmanvaihto
  {:paaiv-jaatymisenesto  (schema/maybe (common-schema/FloatBase -20.0 10.0))
   :paaiv-poisto          (schema/maybe common-schema/FloatPos)
   :kuvaus-sv             (schema/maybe common-schema/String75)
   :kuvaus-fi             (schema/maybe common-schema/String75)
   :ivjarjestelma-poisto  (schema/maybe common-schema/FloatPos)
   :ivjarjestelma-tulo    (schema/maybe common-schema/FloatPos)
   :erillispoistot-poisto (schema/maybe common-schema/FloatPos)
   :erillispoistot-sfp    (schema/maybe common-schema/Float10)
   :erillispoistot-tulo   (schema/maybe common-schema/FloatPos)
   :paaiv-lampotilasuhde  (schema/maybe common-schema/Float1)
   :ivjarjestelma-sfp     (schema/maybe common-schema/Float10)
   :paaiv-sfp             (schema/maybe common-schema/Float10)
   :lto-vuosihyotysuhde   (schema/maybe common-schema/Float1)
   :paaiv-tulo            (schema/maybe common-schema/FloatPos)})


(def Hyotysuhde
  {:tuoton-hyotysuhde (schema/maybe common-schema/FloatPos),
   :jaon-hyotysuhde   (schema/maybe common-schema/FloatPos),
   :lampokerroin      (schema/maybe common-schema/FloatPos),
   :apulaitteet       (schema/maybe common-schema/FloatPos)})


(def MaaraTuotto
  {:maara  common-schema/Integer100,
   :tuotto common-schema/FloatPos})

(def LahtotiedotLammitys
  {:kuvaus-fi         (schema/maybe common-schema/String75)
   :kuvaus-sv         (schema/maybe common-schema/String75)
   :tilat-ja-iv       Hyotysuhde
   :lammin-kayttovesi Hyotysuhde
   :takka             MaaraTuotto
   :ilmanlampopumppu  MaaraTuotto})


(def SisKuorma
  {:selite-fi         (schema/maybe common-schema/String35)
   :selite-sv         (schema/maybe common-schema/String35)
   :kayttoaste        (schema/maybe (common-schema/FloatBase 0.1 1.0))
   :henkilot          (schema/maybe (common-schema/FloatBase 1.0 14.0))
   :kuluttajalaitteet (schema/maybe (common-schema/FloatBase 0.0 12.0))
   :valaistus         (schema/maybe (common-schema/FloatBase 0.0 19.0))})


(def Lahtotiedot
  {:lammitetty-nettoala  common-schema/FloatPos
   :rakennusvaippa       LahtotiedotRakenneusvaippa
   :ikkunat              LahtotiedotIkkunat
   :ilmanvaihto          LahtotiedotIlmanvaihto
   :lammitys             LahtotiedotLammitys
   :jaahdytysjarjestelma {:jaahdytyskauden-painotettu-kylmakerroin (schema/maybe (common-schema/FloatBase 1.0 10.0))}
   :lkvn-kaytto          {:kulutus-per-nelio (schema/maybe common-schema/FloatPos)
                          :vuosikulutus      (schema/maybe common-schema/FloatPos)}
   :sis-kuorma           [SisKuorma]})


(def SahkoLampo
  {:sahko (schema/maybe common-schema/FloatPos)
   :lampo (schema/maybe common-schema/FloatPos)})


(def Tulokset
  {:kaytettavat-energiamuodot    [{:energiamuoto-vakio   (schema/enum "fossiilinen polttoaine"
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
                                  :kayttoveden-valmistus                (schema/maybe common-schema/FloatPos)
                                  :iv-sahko                             (schema/maybe common-schema/FloatPos)
                                  :jaahdytys                            (schema/maybe common-schema/FloatPos)
                                  :kuluttajalaitteet-ja-valaistus-sahko (schema/maybe common-schema/FloatPos)},
   :nettotarve                   {:tilojen-lammitys-vuosikulutus      (schema/maybe common-schema/FloatPos)
                                  :ilmanvaihdon-lammitys-vuosikulutus (schema/maybe common-schema/FloatPos)
                                  :kayttoveden-valmistus-vuosikulutus (schema/maybe common-schema/FloatPos)
                                  :jaahdytys-vuosikulutus             (schema/maybe common-schema/FloatPos)},
   :lampokuormat                 {:aurinko           (schema/maybe common-schema/FloatPos)
                                  :ihmiset           (schema/maybe common-schema/FloatPos)
                                  :kuluttajalaitteet (schema/maybe common-schema/FloatPos)
                                  :valaistus         (schema/maybe common-schema/FloatPos),
                                  :kvesi             (schema/maybe common-schema/FloatPos)},
   :laskentatyokalu              (schema/maybe common-schema/String60)})


(def ToteutunutOstoenergiankulutus
  {:ostettu-energia                         {:kaukolampo-vuosikulutus      (schema/maybe common-schema/FloatPos),
                                             :kokonaissahko-vuosikulutus   (schema/maybe common-schema/FloatPos),
                                             :kiinteistosahko-vuosikulutus (schema/maybe common-schema/FloatPos),
                                             :kayttajasahko-vuosikulutus   (schema/maybe common-schema/FloatPos),
                                             :kaukojaahdytys-vuosikulutus  (schema/maybe common-schema/FloatPos)},
   :ostetut-polttoaineet                    {:kevyt-polttooljy      (schema/maybe common-schema/FloatPos),
                                             :pilkkeet-havu-sekapuu (schema/maybe common-schema/FloatPos),
                                             :pilkkeet-koivu        (schema/maybe common-schema/FloatPos),
                                             :puupelletit           (schema/maybe common-schema/FloatPos),
                                             :vapaa                 {:nimi           common-schema/String30,
                                                                     :yksikko        common-schema/String12,
                                                                     :muunnoskerroin common-schema/FloatPos,
                                                                     :maara-vuodessa common-schema/FloatPos}},
   :to-sahko-vuosikulutus-yhteensa          (schema/maybe common-schema/FloatPos),
   :to-kaukolampo-vuosikulutus-yhteensa     (schema/maybe common-schema/FloatPos),
   :to-polttoaineet-vuosikulutus-yhteensa   (schema/maybe common-schema/FloatPos),
   :to-kaukojaahdytys-vuosikulutus-yhteensa (schema/maybe common-schema/FloatPos)})


(def Huomio
  {:teksti-fi  (schema/maybe common-schema/String1000)
   :teksti-sv  (schema/maybe common-schema/String1000),
   :toimenpide {:nimi-fi       (schema/maybe common-schema/String100)
                :nimi-sv       (schema/maybe common-schema/String100)
                :lampo         (schema/maybe common-schema/FloatPos),
                :sahko         (schema/maybe common-schema/FloatPos),
                :jaahdytys     (schema/maybe common-schema/FloatPos),
                :eluvun-muutos (schema/maybe common-schema/FloatPos)}})


(def Huomiot
  {:suositukset-fi    (schema/maybe common-schema/String1500)
   :suositukset-sv    (schema/maybe common-schema/String1500)
   :lisatietoja-fi    (schema/maybe common-schema/String500)
   :lisatietoja-sv    (schema/maybe common-schema/String500)
   :iv-ilmastointi    Huomio
   :valaistus-muut    Huomio
   :lammitys          Huomio
   :ymparys           Huomio
   :alapohja-ylapohja Huomio})

(def Energiatodistus
  {:perustiedot                    Perustiedot
   :lahtotiedot                    Lahtotiedot
   :tulokset                       Tulokset
   :toteutunut-ostoenergiankulutus ToteutunutOstoenergiankulutus
   :huomiot                        Huomiot
   :lisamerkintoja-fi              (schema/maybe common-schema/String6300)
   :lisamerkintoja-sv              (schema/maybe common-schema/String6300)})
