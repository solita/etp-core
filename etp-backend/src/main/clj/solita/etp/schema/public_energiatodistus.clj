(ns solita.etp.schema.public-energiatodistus
  (:require [schema.core :as schema]
            [solita.etp.schema.energiatodistus :as energiatodistus-schema]
            [solita.etp.schema.common :as common-schema]))

(def PublicPerustiedot
  (select-keys energiatodistus-schema/Perustiedot
               [:katuosoite-fi :katuosoite-sv :valmistumisvuosi
                :havainnointikaynti :rakennustunnus :postinumero
                :keskeiset-suositukset-fi :keskeiset-suositukset-sv
                :laatimisvaihe :yritys :kieli :nimi :kayttotarkoitus]))

(def PublicLahtotiedotIlmanvaihto
  (select-keys energiatodistus-schema/LahtotiedotIlmanvaihto
               [:tyyppi-id :kuvaus-fi :kuvaus-sv]))

(def PublicLahtotiedotLammitys
  (select-keys energiatodistus-schema/LahtotiedotLammitys
               [:lammitysmuoto-1 :lammitysmuoto-2 :lammonjako]))

(def PublicLahtotiedot
  (-> energiatodistus-schema/Lahtotiedot
      (select-keys [:lammitetty-nettoala])
      (assoc :ilmanvaihto PublicLahtotiedotIlmanvaihto)
      (assoc :lammitys PublicLahtotiedotLammitys)))

(def PublicTulokset
  (select-keys energiatodistus-schema/Tulokset
               [:kaytettavat-energiamuodot]))

(defn public-energiatodistus-versio [versio]
  (-> (energiatodistus-schema/energiatodistus-versio
       versio
       (energiatodistus-schema/optional-properties
        {:perustiedot PublicPerustiedot
         :lahtotiedot PublicLahtotiedot
         :tulokset    PublicTulokset}))
      (dissoc :allekirjoitusaika :laskutusaika :korvaava-energiatodistus-id)))

(def PublicEnergiatodistus2013
  (-> (public-energiatodistus-versio 2013)
      energiatodistus-schema/dissoc-not-in-2013))

(def PublicEnergiatodistus2018 (public-energiatodistus-versio 2018))

(def PublicEnergiatodistus
  (schema/conditional
   (partial energiatodistus-schema/versio? 2013) PublicEnergiatodistus2013
   (partial energiatodistus-schema/versio? 2018) PublicEnergiatodistus2018))
