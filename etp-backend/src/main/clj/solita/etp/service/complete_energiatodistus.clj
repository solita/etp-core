(ns solita.etp.service.complete-energiatodistus
  (:require [solita.etp.service.energiatodistus :as energiatodistus-service]
            [solita.etp.service.kayttotarkoitus :as kayttotarkoitus-service]
            [solita.etp.service.e-luokka :as e-luokka-service]))

(defn combine-keys [m f nil-replacement path-new & paths]
  (let [vals (map #(or (get-in m %) nil-replacement) paths)]
    (if (not-any? nil? vals)
      (assoc-in m path-new (apply f vals))
      m)))

(defn assoc-div-nettoala [energiatodistus path]
  (let [new-k (-> path last name (str "-nettoala") keyword)
        new-path (-> path pop (conj new-k))]
    (combine-keys energiatodistus
                  /
                  nil
                  new-path
                  path
                  [:lahtotiedot :lammitetty-nettoala])))

(defn copy-field [m from-path to-path]
  (let [v (get-in m from-path)]
    (assoc-in m to-path v)))

(defn find-complete-energiatodistus* [db energiatodistus kielisyydet
                                      laatimisvaiheet alakayttotarkoitukset]
  (with-precision 20
    (let [{:keys [perustiedot versio]} energiatodistus
          kieli-id (:kieli perustiedot)
          kielisyys (->> kielisyydet (filter #(= (:id %) kieli-id)) first)
          laatimisvaihe-id (:laatimisvaihe perustiedot)
          laatimisvaihe (->> laatimisvaiheet
                             (filter #(= (:id %) laatimisvaihe-id))
                             first)

          ;; Käyttötarkoitus is actually alakäyttötarkoitus in database
          alakayttotarkoitus-id (:kayttotarkoitus perustiedot)
          alakayttotarkoitus (->> (get alakayttotarkoitukset versio)
                                  (filter #(= (:id %) alakayttotarkoitus-id))
                                  first)]
      (-> energiatodistus
          (assoc-in [:perustiedot :kieli-fi] (:label-fi kielisyys))
          (assoc-in [:perustiedot :kieli-sv] (:label-sv kielisyys))
          (assoc-in [:perustiedot :laatimisvaihe-fi] (:label-fi laatimisvaihe))
          (assoc-in [:perustiedot :laatimisvaihe-sv] (:label-sv laatimisvaihe))
          (assoc-in [:perustiedot :alakayttotarkoitus-fi] (:label-fi alakayttotarkoitus))
          (assoc-in [:perustiedot :alakayttotarkoitus-sv] (:label-sv alakayttotarkoitus))
          (assoc-in [:tulokset :kaytettavat-energiamuodot :kaukolampo-kerroin] 0.5)
          (assoc-in [:tulokset :kaytettavat-energiamuodot :sahko-kerroin] 1.2)
          (assoc-in [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-kerroin] 0.5)
          (assoc-in [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-kerroin] 1)
          (assoc-in [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-kerroin] 0.28)
          (assoc-div-nettoala [:tulokset :kaytettavat-energiamuodot :kaukolampo])
          (assoc-div-nettoala [:tulokset :kaytettavat-energiamuodot :sahko])
          (assoc-div-nettoala [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine])
          (assoc-div-nettoala [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine])
          (assoc-div-nettoala [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys])
          (assoc-div-nettoala [:tulokset :kaytettavat-energiamuodot :muu 0 :ostoenergia])
          (assoc-div-nettoala [:tulokset :kaytettavat-energiamuodot :muu 1 :ostoenergia])
          (assoc-div-nettoala [:tulokset :kaytettavat-energiamuodot :muu 2 :ostoenergia])
          (combine-keys *
                        nil
                        [:tulokset :kaytettavat-energiamuodot :kaukolampo-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :kaukolampo]
                        [:tulokset :kaytettavat-energiamuodot :kaukolampo-kerroin])
          (combine-keys *
                        nil
                        [:tulokset :kaytettavat-energiamuodot :kaukolampo-nettoala-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :kaukolampo-nettoala]
                        [:tulokset :kaytettavat-energiamuodot :kaukolampo-kerroin])
          (combine-keys *
                        nil
                        [:tulokset :kaytettavat-energiamuodot :sahko-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :sahko]
                        [:tulokset :kaytettavat-energiamuodot :sahko-kerroin])
          (combine-keys *
                        nil
                        [:tulokset :kaytettavat-energiamuodot :sahko-nettoala-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :sahko-nettoala]
                        [:tulokset :kaytettavat-energiamuodot :sahko-kerroin])
          (combine-keys *
                        nil
                        [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine]
                        [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-kerroin])
          (combine-keys *
                        nil
                        [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-nettoala-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-nettoala]
                        [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-kerroin])
          (combine-keys *
                        nil
                        [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine]
                        [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-kerroin])
          (combine-keys *
                        nil
                        [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-nettoala-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-nettoala]
                        [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-kerroin])
          (combine-keys *
                        nil
                        [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys]
                        [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-kerroin])
          (combine-keys *
                        nil
                        [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-nettoala-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-nettoala]
                        [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-kerroin])
          (combine-keys *
                        nil
                        [:tulokset :kaytettavat-energiamuodot :muu 0 :ostoenergia-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :muu 0 :ostoenergia]
                        [:tulokset :kaytettavat-energiamuodot :muu 0 :muotokerroin])
          (combine-keys *
                        nil
                        [:tulokset :kaytettavat-energiamuodot :muu 0 :ostoenergia-nettoala-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :muu 0 :ostoenergia-nettoala]
                        [:tulokset :kaytettavat-energiamuodot :muu 0 :muotokerroin])
          (combine-keys *
                        nil
                        [:tulokset :kaytettavat-energiamuodot :muu 1 :ostoenergia-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :muu 1 :ostoenergia]
                        [:tulokset :kaytettavat-energiamuodot :muu 1 :muotokerroin])
          (combine-keys *
                        nil
                        [:tulokset :kaytettavat-energiamuodot :muu 1 :ostoenergia-nettoala-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :muu 1 :ostoenergia-nettoala]
                        [:tulokset :kaytettavat-energiamuodot :muu 1 :muotokerroin])
          (combine-keys *
                        nil
                        [:tulokset :kaytettavat-energiamuodot :muu 2 :ostoenergia-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :muu 2 :ostoenergia]
                        [:tulokset :kaytettavat-energiamuodot :muu 2 :muotokerroin])
          (combine-keys *
                        nil
                        [:tulokset :kaytettavat-energiamuodot :muu 2 :ostoenergia-nettoala-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :muu 2 :ostoenergia-nettoala]
                        [:tulokset :kaytettavat-energiamuodot :muu 2 :muotokerroin])
          (combine-keys +
                        0
                        [:tulokset :kaytettavat-energiamuodot :summa]
                        [:tulokset :kaytettavat-energiamuodot :kaukolampo]
                        [:tulokset :kaytettavat-energiamuodot :sahko]
                        [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine]
                        [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys]
                        [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine]
                        [:tulokset :kaytettavat-energiamuodot :muu 0 :ostoenergia]
                        [:tulokset :kaytettavat-energiamuodot :muu 1 :ostoenergia]
                        [:tulokset :kaytettavat-energiamuodot :muu 2 :ostoenergia])
          (combine-keys +
                        0
                        [:tulokset :kaytettavat-energiamuodot :kertoimella-summa]
                        [:tulokset :kaytettavat-energiamuodot :kaukolampo-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :sahko-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :muu 0 :ostoenergia-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :muu 1 :ostoenergia-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :muu 2 :ostoenergia-kertoimella])
          (combine-keys (comp #(Math/ceil %) +)
                        0
                        [:tulokset :kaytettavat-energiamuodot :nettoala-kertoimella-summa]
                        [:tulokset :kaytettavat-energiamuodot :kaukolampo-nettoala-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :sahko-nettoala-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-nettoala-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-nettoala-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-nettoala-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :muu 0 :ostoenergia-nettoala-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :muu 1 :ostoenergia-nettoala-kertoimella]
                        [:tulokset :kaytettavat-energiamuodot :muu 2 :ostoenergia-nettoala-kertoimella])
          (copy-field [:tulokset :kaytettavat-energiamuodot :nettoala-kertoimella-summa]
                      [:tulokset :e-luku])
          (combine-keys (fn [nettoala e-luku]
                          (e-luokka-service/find-e-luokka-info db
                                                               versio
                                                               alakayttotarkoitus-id
                                                               nettoala
                                                               e-luku))
                        nil
                        [:tulokset :e-luokka-info]
                        [:lahtotiedot :lammitetty-nettoala]
                        [:tulokset :e-luku])
          (combine-keys *
                        nil
                        [:lahtotiedot :rakennusvaippa :ulkoseinat :UA]
                        [:lahtotiedot :rakennusvaippa :ulkoseinat :ala]
                        [:lahtotiedot :rakennusvaippa :ulkoseinat :U])
          (combine-keys *
                        nil
                        [:lahtotiedot :rakennusvaippa :ylapohja :UA]
                        [:lahtotiedot :rakennusvaippa :ylapohja :ala]
                        [:lahtotiedot :rakennusvaippa :ylapohja :U])
          (combine-keys *
                        nil
                        [:lahtotiedot :rakennusvaippa :alapohja :UA]
                        [:lahtotiedot :rakennusvaippa :alapohja :ala]
                        [:lahtotiedot :rakennusvaippa :alapohja :U])
          (combine-keys *
                        nil
                        [:lahtotiedot :rakennusvaippa :ikkunat :UA]
                        [:lahtotiedot :rakennusvaippa :ikkunat :ala]
                        [:lahtotiedot :rakennusvaippa :ikkunat :U])
          (combine-keys *
                        nil
                        [:lahtotiedot :rakennusvaippa :ulkoovet :UA]
                        [:lahtotiedot :rakennusvaippa :ulkoovet :ala]
                        [:lahtotiedot :rakennusvaippa :ulkoovet :U])
          (combine-keys +
                        0
                        [:lahtotiedot :rakennusvaippa :UA-summa]
                        [:lahtotiedot :rakennusvaippa :ulkoseinat :UA]
                        [:lahtotiedot :rakennusvaippa :ylapohja :UA]
                        [:lahtotiedot :rakennusvaippa :alapohja :UA]
                        [:lahtotiedot :rakennusvaippa :ikkunat :UA]
                        [:lahtotiedot :rakennusvaippa :ulkoovet :UA]
                        [:lahtotiedot :rakennusvaippa :kylmasillat-UA])
          (combine-keys /
                        nil
                        [:lahtotiedot :rakennusvaippa :ulkoseinat :osuus-lampohaviosta]
                        [:lahtotiedot :rakennusvaippa :ulkoseinat :UA]
                        [:lahtotiedot :rakennusvaippa :UA-summa])
          (combine-keys /
                        nil
                        [:lahtotiedot :rakennusvaippa :ylapohja :osuus-lampohaviosta]
                        [:lahtotiedot :rakennusvaippa :ylapohja :UA]
                        [:lahtotiedot :rakennusvaippa :UA-summa])
          (combine-keys /
                        nil
                        [:lahtotiedot :rakennusvaippa :alapohja :osuus-lampohaviosta]
                        [:lahtotiedot :rakennusvaippa :alapohja :UA]
                        [:lahtotiedot :rakennusvaippa :UA-summa])
          (combine-keys /
                        nil
                        [:lahtotiedot :rakennusvaippa :ikkunat :osuus-lampohaviosta]
                        [:lahtotiedot :rakennusvaippa :ikkunat :UA]
                        [:lahtotiedot :rakennusvaippa :UA-summa])
          (combine-keys /
                        nil
                        [:lahtotiedot :rakennusvaippa :ulkoovet :osuus-lampohaviosta]
                        [:lahtotiedot :rakennusvaippa :ulkoovet :UA]
                        [:lahtotiedot :rakennusvaippa :UA-summa])
          (combine-keys /
                        nil
                        [:lahtotiedot :rakennusvaippa :kylmasillat-osuus-lampohaviosta]
                        [:lahtotiedot :rakennusvaippa :kylmasillat-UA]
                        [:lahtotiedot :rakennusvaippa :UA-summa])
          (combine-keys #(str %1 " / " %2)
                        nil
                        [:lahtotiedot :ilmanvaihto :paaiv :tulo-poisto]
                        [:lahtotiedot :ilmanvaihto :paaiv :tulo]
                        [:lahtotiedot :ilmanvaihto :paaiv :poisto])
          (combine-keys #(str %1 " / " %2)
                        nil
                        [:lahtotiedot :ilmanvaihto :erillispoistot :tulo-poisto]
                        [:lahtotiedot :ilmanvaihto :erillispoistot :tulo]
                        [:lahtotiedot :ilmanvaihto :erillispoistot :poisto])
          (combine-keys #(str %1 " / " %2)
                        nil
                        [:lahtotiedot :ilmanvaihto :ivjarjestelma :tulo-poisto]
                        [:lahtotiedot :ilmanvaihto :ivjarjestelma :tulo]
                        [:lahtotiedot :ilmanvaihto :ivjarjestelma :poisto])
          (assoc-div-nettoala [:tulokset :uusiutuvat-omavaraisenergiat :aurinkosahko])
          (assoc-div-nettoala [:tulokset :uusiutuvat-omavaraisenergiat :tuulisahko])
          (assoc-div-nettoala [:tulokset :uusiutuvat-omavaraisenergiat :aurinkolampo])
          (assoc-div-nettoala [:tulokset :uusiutuvat-omavaraisenergiat :muulampo])
          (assoc-div-nettoala [:tulokset :uusiutuvat-omavaraisenergiat :muusahko])
          (assoc-div-nettoala [:tulokset :uusiutuvat-omavaraisenergiat :lampopumppu])
          (assoc-div-nettoala [:tulokset :uusiutuvat-omavaraisenergiat 0 :vuosikulutus])
          (assoc-div-nettoala [:tulokset :uusiutuvat-omavaraisenergiat 1 :vuosikulutus])
          (assoc-div-nettoala [:tulokset :uusiutuvat-omavaraisenergiat 2 :vuosikulutus])
          (assoc-div-nettoala [:tulokset :uusiutuvat-omavaraisenergiat 3 :vuosikulutus])
          (assoc-div-nettoala [:tulokset :uusiutuvat-omavaraisenergiat 4 :vuosikulutus])
          (assoc-div-nettoala [:tulokset :uusiutuvat-omavaraisenergiat 5 :vuosikulutus])
          (combine-keys +
                        0
                        [:tulokset :tekniset-jarjestelmat :sahko-summa]
                        [:tulokset :tekniset-jarjestelmat :tilojen-lammitys :sahko]
                        [:tulokset :tekniset-jarjestelmat :tuloilman-lammitys :sahko]
                        [:tulokset :tekniset-jarjestelmat :kayttoveden-valmistus :sahko]
                        [:tulokset :tekniset-jarjestelmat :iv-sahko]
                        [:tulokset :tekniset-jarjestelmat :jaahdytys :sahko]
                        [:tulokset :tekniset-jarjestelmat :kuluttajalaitteet-ja-valaistus-sahko])
          (combine-keys +
                        0
                        [:tulokset :tekniset-jarjestelmat :lampo-summa]
                        [:tulokset :tekniset-jarjestelmat :tilojen-lammitys :lampo]
                        [:tulokset :tekniset-jarjestelmat :tuloilman-lammitys :lampo]
                        [:tulokset :tekniset-jarjestelmat :kayttoveden-valmistus :lampo]
                        [:tulokset :tekniset-jarjestelmat :jaahdytys :lampo])

          (combine-keys +
                        0
                        [:tulokset :tekniset-jarjestelmat :kaukojaahdytys-summa]
                        [:tulokset :tekniset-jarjestelmat :jaahdytys :kaukojaahdytys])


          (assoc-div-nettoala [:tulokset :nettotarve :tilojen-lammitys-vuosikulutus])
          (assoc-div-nettoala [:tulokset :nettotarve :ilmanvaihdon-lammitys-vuosikulutus])
          (assoc-div-nettoala [:tulokset :nettotarve :kayttoveden-valmistus-vuosikulutus])
          (assoc-div-nettoala [:tulokset :nettotarve :jaahdytys-vuosikulutus])
          (assoc-div-nettoala [:tulokset :lampokuormat :aurinko])
          (assoc-div-nettoala [:tulokset :lampokuormat :ihmiset])
          (assoc-div-nettoala [:tulokset :lampokuormat :kuluttajalaitteet])
          (assoc-div-nettoala [:tulokset :lampokuormat :valaistus])
          (assoc-div-nettoala [:tulokset :lampokuormat :kvesi])
          (assoc-div-nettoala [:toteutunut-ostoenergiankulutus :ostettu-energia :kaukolampo-vuosikulutus])
          (assoc-div-nettoala [:toteutunut-ostoenergiankulutus :ostettu-energia :kokonaissahko-vuosikulutus])
          (assoc-div-nettoala [:toteutunut-ostoenergiankulutus :ostettu-energia :kiinteistosahko-vuosikulutus])
          (assoc-div-nettoala [:toteutunut-ostoenergiankulutus :ostettu-energia :kayttajasahko-vuosikulutus])
          (assoc-div-nettoala [:toteutunut-ostoenergiankulutus :ostettu-energia :kaukojaahdytys-vuosikulutus])
          (assoc-div-nettoala [:toteutunut-ostoenergiankulutus :ostettu-energia :muu 0 :vuosikulutus])
          (assoc-div-nettoala [:toteutunut-ostoenergiankulutus :ostettu-energia :muu 1 :vuosikulutus])
          (assoc-div-nettoala [:toteutunut-ostoenergiankulutus :ostettu-energia :muu 2 :vuosikulutus])
          (assoc-div-nettoala [:toteutunut-ostoenergiankulutus :ostettu-energia :muu 3 :vuosikulutus])
          (assoc-div-nettoala [:toteutunut-ostoenergiankulutus :ostettu-energia :muu 4 :vuosikulutus])
          (assoc-in [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :kevyt-polttooljy-kerroin] 10)
          (assoc-in [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-havu-sekapuu-kerroin] 1300)
          (assoc-in [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-koivu-kerroin] 1700)
          (assoc-in [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :puupelletit-kerroin] 4.7)
          (combine-keys *
                        nil
                        [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :kevyt-polttooljy-kwh]
                        [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :kevyt-polttooljy]
                        [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :kevyt-polttooljy-kerroin])
          (combine-keys *
                        nil
                        [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-havu-sekapuu-kwh]
                        [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-havu-sekapuu]
                        [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-havu-sekapuu-kerroin])
          (combine-keys *
                        nil
                        [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-koivu-kwh]
                        [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-koivu]
                        [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-koivu-kerroin])
          (combine-keys *
                        nil
                        [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :puupelletit-kwh]
                        [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :puupelletit]
                        [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :puupelletit-kerroin])
          (assoc-div-nettoala [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :kevyt-polttooljy-kwh])
          (assoc-div-nettoala [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-havu-sekapuu-kwh])
          (assoc-div-nettoala [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-koivu-kwh])
          (assoc-div-nettoala [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :puupelletit-kwh])
          (update-in [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :muu]
                     (fn [vapaat]
                       (if-let [nettoala (-> energiatodistus :lahtotiedot :lammitetty-nettoala)]
                         (mapv (fn [{:keys [maara-vuodessa muunnoskerroin] :as vapaa}]
                                 (if (and maara-vuodessa muunnoskerroin)
                                   (let [kwh (* maara-vuodessa muunnoskerroin)]
                                     (assoc vapaa :kwh kwh :kwh-nettoala (/ kwh nettoala)))
                                   vapaa))
                               vapaat)
                         vapaat)))
          (assoc-div-nettoala [:toteutunut-ostoenergiankulutus :sahko-vuosikulutus-yhteensa])
          (assoc-div-nettoala [:toteutunut-ostoenergiankulutus :kaukolampo-vuosikulutus-yhteensa])
          (assoc-div-nettoala [:toteutunut-ostoenergiankulutus :polttoaineet-vuosikulutus-yhteensa])
          (assoc-div-nettoala [:toteutunut-ostoenergiankulutus :kaukojaahdytys-vuosikulutus-yhteensa])
          (combine-keys +
                        0
                        [:toteutunut-ostoenergiankulutus :summa]
                        [:toteutunut-ostoenergiankulutus :sahko-vuosikulutus-yhteensa]
                        [:toteutunut-ostoenergiankulutus :kaukolampo-vuosikulutus-yhteensa]
                        [:toteutunut-ostoenergiankulutus :polttoaineet-vuosikulutus-yhteensa]
                        [:toteutunut-ostoenergiankulutus :kaukojaahdytys-vuosikulutus-yhteensa])
          (combine-keys +
                        0
                        [:toteutunut-ostoenergiankulutus :summa-nettoala]
                        [:toteutunut-ostoenergiankulutus :sahko-vuosikulutus-yhteensa-nettoala]
                        [:toteutunut-ostoenergiankulutus :kaukolampo-vuosikulutus-yhteensa-nettoala]
                        [:toteutunut-ostoenergiankulutus :polttoaineet-vuosikulutus-yhteensa-nettoala]
                        [:toteutunut-ostoenergiankulutus :kaukojaahdytys-vuosikulutus-yhteensa-nettoala])))))

(defn required-luokittelut [db]
  {:kielisyydet (energiatodistus-service/find-kielisyys)
   :laatimisvaiheet (energiatodistus-service/find-laatimisvaiheet)
   :alakayttotarkoitukset (reduce #(assoc %1 %2 (kayttotarkoitus-service/find-alakayttotarkoitukset db %2))
                                  {}
                                  [2013 2018])})

(defn find-complete-energiatodistus
  ([db id]
   (find-complete-energiatodistus db nil id))
  ([db whoami id]
   (let [{:keys [kielisyydet laatimisvaiheet alakayttotarkoitukset]}
         (required-luokittelut db)]
     (find-complete-energiatodistus*
      db
      (if whoami
        (energiatodistus-service/find-energiatodistus db whoami id)
        (energiatodistus-service/find-energiatodistus db id))
      kielisyydet
      laatimisvaiheet
      alakayttotarkoitukset))))

(defn find-complete-energiatodistukset-by-laatija [db laatija-id tila-id]
  (let [{:keys [kielisyydet laatimisvaiheet alakayttotarkoitukset]}
        (required-luokittelut db)]
    (->> (energiatodistus-service/find-energiatodistukset-by-laatija db
                                                                     laatija-id
                                                                     tila-id)
         (map #(find-complete-energiatodistus* db
                                               %
                                               kielisyydet
                                               laatimisvaiheet
                                               alakayttotarkoitukset)))))
