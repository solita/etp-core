(ns solita.etp.schema.energiatodistus
  (:require [solita.common.map :as m]
            [schema.core :as schema]
            [solita.etp.schema.common :as common-schema]
            [solita.common.schema :as xschema]
            [solita.etp.schema.geo :as geo-schema]
            [clojure.string :as str])
  (:import (schema.core Predicate EnumSchema Constrained)))

(defn optional-properties [schema]
  (m/map-values
    #(cond
       (xschema/maybe? %) %
       (instance? Constrained %) (schema/maybe %)
       (instance? EnumSchema %) (schema/maybe %)
       (instance? Predicate %) (schema/maybe %)
       (class? %) (schema/maybe %)
       (map? %) (optional-properties %)
       (vector? %) (mapv optional-properties %)
       (coll? %) (map optional-properties %))
    schema))

(defn valid-rakennustunnus? [s]
  (try
    (let [s           (str/lower-case s)
          number-part (subs s 0 9)
          checksum    (last s)]
      (and (= 10 (count s))
           (= checksum (common-schema/henkilotunnus-checksum number-part))))
    (catch StringIndexOutOfBoundsException _ false)))

(def Rakennustunnus
  (schema/constrained schema/Str valid-rakennustunnus?))

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
   :laatimisvaihe            common-schema/Key
   :kiinteistotunnus         common-schema/String50
   :yritys                   Yritys
   :tilaaja                  common-schema/String200
   :rakennusosa              common-schema/String100
   :kieli                    common-schema/Key
   :nimi                     common-schema/String50

   ;; This in in fact alakäyttötarkoitus in database
   :kayttotarkoitus          schema/Str})

(defn Rakenneusvaippa [mininclusive maxinclusive]
  {:ala common-schema/FloatPos
   :U (common-schema/FloatBase mininclusive maxinclusive)})

(def LahtotiedotRakennusvaippa
  {:ilmanvuotoluku common-schema/Float50
   :lampokapasiteetti common-schema/FloatPos
   :ilmatilavuus common-schema/FloatPos

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
   :luode     LahtotiedotIkkuna
   :valokupu  LahtotiedotIkkuna
   :katto     LahtotiedotIkkuna})

(def PoistoTuloSfp
  {:poisto common-schema/FloatPos
   :tulo   common-schema/FloatPos
   :sfp    common-schema/Float10})

(def LahtotiedotIlmanvaihto
  {:erillispoistot      PoistoTuloSfp
   :ivjarjestelma       PoistoTuloSfp
   :tyyppi-id           common-schema/Key
   :kuvaus-fi           common-schema/String75
   :kuvaus-sv           common-schema/String75
   :lto-vuosihyotysuhde common-schema/Float1
   :tuloilma-lampotila  common-schema/FloatPos
   :paaiv               (merge PoistoTuloSfp {:lampotilasuhde common-schema/Float1
                                              :jaatymisenesto (common-schema/FloatBase -20.0 10.0)})})

(def Hyotysuhde
  {:tuoton-hyotysuhde common-schema/FloatPos,
   :jaon-hyotysuhde   common-schema/FloatPos,
   :lampokerroin      common-schema/FloatPos,
   :apulaitteet       common-schema/FloatPos,
   :lampohavio-lammittamaton-tila common-schema/FloatPos,
   :lampopumppu-tuotto-osuus common-schema/Float1})

(def MaaraTuotto
  {:maara  common-schema/Integer100,
   :tuotto common-schema/FloatPos})

(def LahtotiedotLammitys
  {:lammitysmuoto-1-id common-schema/Key
   :lammitysmuoto-2-id common-schema/Key
   :lammonjako-id      common-schema/Key
   :kuvaus-fi          common-schema/String75
   :kuvaus-sv          common-schema/String75
   :tilat-ja-iv        Hyotysuhde
   :lammin-kayttovesi  Hyotysuhde
   :takka              MaaraTuotto
   :ilmanlampopumppu   MaaraTuotto})

(defn SisKuorma [mininclusive maxinclusive]
  {:kayttoaste        (common-schema/FloatBase 0.1 1.0)
   :lampokuorma       (common-schema/FloatBase mininclusive maxinclusive)})

(def Lahtotiedot
  {:lammitetty-nettoala  common-schema/FloatPos
   :rakennusvaippa       LahtotiedotRakennusvaippa
   :ikkunat              LahtotiedotIkkunat
   :ilmanvaihto          LahtotiedotIlmanvaihto
   :lammitys             LahtotiedotLammitys
   :jaahdytysjarjestelma {:jaahdytyskauden-painotettu-kylmakerroin (common-schema/FloatBase 1.0 10.0)}
   :lkvn-kaytto          {:ominaiskulutus common-schema/FloatPos
                          :lammitysenergian-nettotarve common-schema/FloatPos}
   :sis-kuorma {
     :henkilot          (SisKuorma 1.0 14.0)
     :kuluttajalaitteet (SisKuorma 0.0 12.0)
     :valaistus         (SisKuorma 0.0 19.0)}
    })

(def UusiutuvatOmavaraisenergiat
  {:aurinkosahko common-schema/FloatPos
   :tuulisahko   common-schema/FloatPos
   :aurinkolampo common-schema/FloatPos
   :muulampo     common-schema/FloatPos
   :muusahko     common-schema/FloatPos
   :lampopumppu  common-schema/FloatPos})

(def SahkoLampo
  {:sahko common-schema/FloatPos
   :lampo common-schema/FloatPos})

(def Kuukausierittely (schema/maybe
                       {:tuotto (optional-properties UusiutuvatOmavaraisenergiat)
                        :kulutus (optional-properties SahkoLampo)}))

(def OptionalKuukausierittely (schema/constrained [Kuukausierittely]
                                                   #(contains? #{0 12} (count %))))

(def Tulokset
  {:kaytettavat-energiamuodot
   {:fossiilinen-polttoaine common-schema/FloatPos
    :sahko                  common-schema/FloatPos
    :kaukojaahdytys         common-schema/FloatPos
    :kaukolampo             common-schema/FloatPos
    :uusiutuva-polttoaine   common-schema/FloatPos},

   :uusiutuvat-omavaraisenergiat
   UusiutuvatOmavaraisenergiat,

   :kuukausierittely OptionalKuukausierittely

   :tekniset-jarjestelmat
   {:tilojen-lammitys                     SahkoLampo,
    :tuloilman-lammitys                   SahkoLampo,
    :kayttoveden-valmistus                SahkoLampo
    :iv-sahko                             common-schema/FloatPos
    :jaahdytys                            (assoc SahkoLampo :kaukojaahdytys common-schema/FloatPos)
    :kuluttajalaitteet-ja-valaistus-sahko common-schema/FloatPos},

   :nettotarve
   {:tilojen-lammitys-vuosikulutus      common-schema/FloatPos
    :ilmanvaihdon-lammitys-vuosikulutus common-schema/FloatPos
    :kayttoveden-valmistus-vuosikulutus common-schema/FloatPos
    :jaahdytys-vuosikulutus             common-schema/FloatPos},

   :lampokuormat
   {:aurinko           common-schema/FloatPos
    :ihmiset           common-schema/FloatPos
    :kuluttajalaitteet common-schema/FloatPos
    :valaistus         common-schema/FloatPos,
    :kvesi             common-schema/FloatPos},

   :laskentatyokalu common-schema/String60})

(def ToteutunutOstoenergiankulutus
  {:ostettu-energia
   {:kaukolampo-vuosikulutus      common-schema/FloatPos,
    :kokonaissahko-vuosikulutus   common-schema/FloatPos,
    :kiinteistosahko-vuosikulutus common-schema/FloatPos,
    :kayttajasahko-vuosikulutus   common-schema/FloatPos,
    :kaukojaahdytys-vuosikulutus  common-schema/FloatPos},

   :ostetut-polttoaineet
   {:kevyt-polttooljy      common-schema/FloatPos,
    :pilkkeet-havu-sekapuu common-schema/FloatPos,
    :pilkkeet-koivu        common-schema/FloatPos,
    :puupelletit           common-schema/FloatPos,
    :muu
      [{:nimi           common-schema/String30,
        :yksikko        common-schema/String12,
        :muunnoskerroin common-schema/FloatPos,
        :maara-vuodessa common-schema/FloatPos}]},

   :sahko-vuosikulutus-yhteensa common-schema/FloatPos,
   :kaukolampo-vuosikulutus-yhteensa common-schema/FloatPos,
   :polttoaineet-vuosikulutus-yhteensa common-schema/FloatPos,
   :kaukojaahdytys-vuosikulutus-yhteensa common-schema/FloatPos})

(def Huomio
  {:teksti-fi  common-schema/String1000
   :teksti-sv  common-schema/String1000,
   :toimenpide [{:nimi-fi       common-schema/String100
                 :nimi-sv       common-schema/String100
                 :lampo         schema/Num
                 :sahko         schema/Num
                 :jaahdytys     schema/Num
                 :eluvun-muutos schema/Num}]})

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
  "This schema is used in
  add-energiatodistus and update-energiatodistus
  services for 2018 version"
  (optional-properties
    {:korvattu-energiatodistus-id    common-schema/Key
     :laskutettava-yritys-id         common-schema/Key
     :laskuriviviite                 common-schema/String50
     :perustiedot                    Perustiedot
     :lahtotiedot                    Lahtotiedot
     :tulokset                       Tulokset
     :toteutunut-ostoenergiankulutus ToteutunutOstoenergiankulutus
     :huomiot                        Huomiot
     :lisamerkintoja-fi              common-schema/String6300
     :lisamerkintoja-sv              common-schema/String6300}))

(defn- dissoc-path [map path]
  (update-in map (butlast path) #(dissoc % (last path))))

(defn dissoc-not-in-2013 [schema2018]
  (-> schema2018
      (dissoc-path [:perustiedot :laatimisvaihe])))

(def UserDefinedEnergiamuoto
  {:nimi common-schema/String50
   :muotokerroin common-schema/FloatPos
   :ostoenergia common-schema/FloatPos})

(def UserDefinedEnergia
  {:nimi-fi common-schema/String50
   :nimi-sv common-schema/String50
   :vuosikulutus common-schema/FloatPos})

(def EnergiatodistusSave2013
  "This schema is used in
  add-energiatodistus and update-energiatodistus
  services for 2013 version"
  (-> (dissoc-not-in-2013 EnergiatodistusSave2018)
      (assoc-in [:perustiedot :uudisrakennus] schema/Bool)
      (assoc-in [:tulokset :kaytettavat-energiamuodot :muu]
                [(optional-properties UserDefinedEnergiamuoto)])
      (assoc-in [:tulokset :uusiutuvat-omavaraisenergiat]
                [(optional-properties UserDefinedEnergia)])
      (assoc-in [:toteutunut-ostoenergiankulutus :ostettu-energia :muu]
                [(optional-properties UserDefinedEnergia)])))

(defn energiatodistus-versio [versio save-schema]
  "Energiatodistus schema contains basic information about persistent energiatodistus"
  (merge common-schema/Id save-schema
    {:versio (schema/eq versio)
     :tila-id common-schema/Key
     :laatija-id common-schema/Key
     :laatija-fullname schema/Str
     :allekirjoitusaika (schema/maybe common-schema/Instant)
     :laskutusaika (schema/maybe common-schema/Instant)
     :korvaava-energiatodistus-id (schema/maybe common-schema/Key)}))

(def Energiatodistus2018
  "Energiatodistus 2018"
  (energiatodistus-versio 2018 EnergiatodistusSave2018))

(def Energiatodistus2013
  "Energiatodistus 2013"
  (energiatodistus-versio 2013 EnergiatodistusSave2013))

(defn versio? [versio et] (-> et :versio (= versio)))

(def Energiatodistus
  (schema/conditional
    (partial versio? 2018) Energiatodistus2018
    (partial versio? 2013) Energiatodistus2013))

(def Signature {:signature schema/Str :chain [schema/Str]})
