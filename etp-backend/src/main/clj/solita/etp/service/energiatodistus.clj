(ns solita.etp.service.energiatodistus
  (:require [solita.etp.db :as db]
            [solita.etp.exception :as exception]
            [solita.etp.schema.energiatodistus :as energiatodistus-schema]
            [solita.etp.service.json :as json]
            [solita.etp.service.rooli :as rooli-service]
            [schema.coerce :as coerce]
            [clojure.java.jdbc :as jdbc]))

; *** Require sql functions ***
(db/require-queries 'energiatodistus)

; *** Conversions from database data types ***
(def coerce-energiatodistus (coerce/coercer energiatodistus-schema/Energiatodistus json/json-coercions))

(def tilat [:draft :in-signing :signed :discarded :replaced :deleted])

(defn tila-key [tila-id] (nth tilat tila-id))

(defn find-energiatodistus
  ([db id]
   (first (map (comp coerce-energiatodistus json/merge-data db/kebab-case-keys)
               (energiatodistus-db/select-energiatodistus db {:id id}))))
  ([db whoami id]
   (let [energiatodistus (find-energiatodistus db id)]
     (if (or (rooli-service/paakayttaja? whoami)
             (and (rooli-service/laatija? whoami)
                  (= (:laatija-id energiatodistus) (:id whoami))))
       energiatodistus
       (exception/throw-forbidden!)))))

(defn find-energiatodistukset-by-laatija [db laatija-id tila-id]
  (map (comp coerce-energiatodistus json/merge-data db/kebab-case-keys)
       (energiatodistus-db/select-energiatodistukset-by-laatija
         db {:laatija-id laatija-id :tila-id tila-id})))

(defn add-energiatodistus! [db whoami versio energiatodistus]
  (:id (energiatodistus-db/insert-energiatodistus<!
         db (assoc (json/data-db-row energiatodistus)
              :versio versio
              :laatija-id (:id whoami)))))

(defn assert-laatija! [whoami energiatodistus]
  (when-not (= (:laatija-id energiatodistus) (:id whoami))
    (exception/throw-forbidden!
      (str "User " (:id whoami) " is not the laatija of et-" (:id energiatodistus)))))

(defn update-energiatodistus-luonnos! [db whoami id energiatodistus]
  (assert-laatija! whoami (find-energiatodistus db id))
  (energiatodistus-db/update-energiatodistus-luonnos! db
     {:id id :data (json/write-value-as-string energiatodistus)}))

(defn delete-energiatodistus-luonnos! [db whoami id]
  (assert-laatija! whoami (find-energiatodistus db id))
  (energiatodistus-db/delete-energiatodistus-luonnos! db {:id id}))

;;
;; Signing process
;;

(defn start-energiatodistus-signing! [db whoami id]
  (let [result (energiatodistus-db/update-energiatodistus-allekirjoituksessa!
                 db {:id id :laatija-id (:id whoami)})]
    (if (= result 1) :ok
      (when-let [{:keys [tila-id] :as et} (find-energiatodistus db id)]
        (assert-laatija! whoami et)
        (case (tila-key tila-id)
          :in-signing :already-in-signing
          :deleted nil
          :already-signed)))))

(defn end-energiatodistus-signing! [db whoami id]
  (let [result (energiatodistus-db/update-energiatodistus-allekirjoitettu!
                 db {:id id :laatija-id (:id whoami)})]
    (if (= result 1) :ok
      (when-let [{:keys [tila-id] :as et} (find-energiatodistus db id)]
        (assert-laatija! whoami et)
        (case (tila-key tila-id)
         :draft :not-in-signing
         :deleted nil
         :already-signed)))))

;;
;; Energiatodistuksen kielisyys
;;

(def kielisyys [{:id 0 :label-fi "Suomi" :label-sv "Finska"}
                {:id 1 :label-fi "Ruotsi" :label-sv "Svenska"}
                {:id 2 :label-fi "Kaksikielinen" :label-sv "Tvåspråkig"}])

(defn find-kielisyys [] kielisyys)

;;
;; Energiatodistuksen laatimisvaihe
;;

(def laatimisvaiheet [{:id 0 :label-fi "Rakennuslupa" :label-sv "Bygglov"}
                      {:id 1 :label-fi "Käyttöönotto" :label-sv "Införandet"}
                      {:id 2 :label-fi "Olemassa oleva rakennus" :label-sv "Befintlig byggnad"}])

(defn find-laatimisvaiheet [] laatimisvaiheet)

;;
;; Energiatodistuksen käyttötarkoitusluokittelu
;;

(defn find-kayttotarkoitukset [db versio]
  (energiatodistus-db/select-kayttotarkoitusluokat-by-versio db {:versio versio}))

(defn find-alakayttotarkoitukset [db versio]
  (energiatodistus-db/select-alakayttotarkoitusluokat-by-versio db {:versio versio}))

;;
;; Energiatodistuksen "denormalisointi" and "laskennalliset kentät""
;;

(defn *-ceil [& args]
  (->> args (apply *) Math/ceil))

(defn div-ceil [& args]
  (->> args (apply /) Math/ceil))

(defn combine-keys [m f nil-replacement path-new & paths]
  (let [vals (map #(or (get-in m %) nil-replacement) paths)]
    (if (not-any? nil? vals)
      (assoc-in m path-new (apply f vals))
      m)))

(defn assoc-div-nettoala [energiatodistus path]
  (let [new-k (-> path last name (str "-nettoala") keyword)
        new-path (-> path pop (conj new-k))]
    (combine-keys energiatodistus
                  div-ceil
                  nil
                  new-path
                  path
                  [:lahtotiedot :lammitetty-nettoala])))

(defn copy-field [energiatodistus from-path to-path]
  (let [v (get-in energiatodistus from-path)]
    (assoc-in energiatodistus to-path v)))

(defn find-complete-energiatodistus* [energiatodistus alakayttotarkoitukset]
  (let [perustiedot (:perustiedot energiatodistus)
        kieli-id (:kieli perustiedot)
        kielisyys (->> (find-kielisyys) (filter #(= (:id %) kieli-id)) first)
        laatimisvaihe-id (:laatimisvaihe perustiedot)
        laatimisvaihe (->> (find-laatimisvaiheet)
                           (filter #(= (:id %) laatimisvaihe-id))
                           first)
        kayttotarkoitus-id (:kayttotarkoitus perustiedot)
        alakayttotarkoitus (->> alakayttotarkoitukset
                                (filter #(= (:id %) kayttotarkoitus-id))
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
        (combine-keys *-ceil
                      nil
                      [:tulokset :kaytettavat-energiamuodot :kaukolampo-kertoimella]
                      [:tulokset :kaytettavat-energiamuodot :kaukolampo]
                      [:tulokset :kaytettavat-energiamuodot :kaukolampo-kerroin])
        (combine-keys *-ceil
                      nil
                      [:tulokset :kaytettavat-energiamuodot :kaukolampo-nettoala-kertoimella]
                      [:tulokset :kaytettavat-energiamuodot :kaukolampo-nettoala]
                      [:tulokset :kaytettavat-energiamuodot :kaukolampo-kerroin])
        (combine-keys *-ceil
                      nil
                      [:tulokset :kaytettavat-energiamuodot :sahko-kertoimella]
                      [:tulokset :kaytettavat-energiamuodot :sahko]
                      [:tulokset :kaytettavat-energiamuodot :sahko-kerroin])
        (combine-keys *-ceil
                      nil
                      [:tulokset :kaytettavat-energiamuodot :sahko-nettoala-kertoimella]
                      [:tulokset :kaytettavat-energiamuodot :sahko-nettoala]
                      [:tulokset :kaytettavat-energiamuodot :sahko-kerroin])
        (combine-keys *-ceil
                      nil
                      [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-kertoimella]
                      [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine]
                      [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-kerroin])
        (combine-keys *-ceil
                      nil
                      [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-nettoala-kertoimella]
                      [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-nettoala]
                      [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-kerroin])
        (combine-keys *-ceil
                      nil
                      [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-kertoimella]
                      [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine]
                      [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-kerroin])
        (combine-keys *-ceil
                      nil
                      [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-nettoala-kertoimella]
                      [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-nettoala]
                      [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-kerroin])
        (combine-keys *-ceil
                      nil
                      [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-kertoimella]
                      [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys]
                      [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-kerroin])
        (combine-keys *-ceil
                      nil
                      [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-nettoala-kertoimella]
                      [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-nettoala]
                      [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-kerroin])
        (combine-keys +
                      0
                      [:tulokset :kaytettavat-energiamuodot :summa]
                      [:tulokset :kaytettavat-energiamuodot :kaukolampo]
                      [:tulokset :kaytettavat-energiamuodot :sahko]
                      [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine]
                      [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys]
                      [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine])
        (combine-keys +
                      0
                      [:tulokset :kaytettavat-energiamuodot :kerroin-summa]
                      [:tulokset :kaytettavat-energiamuodot :kaukolampo-kerroin]
                      [:tulokset :kaytettavat-energiamuodot :sahko-kerroin]
                      [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-kerroin]
                      [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-kerroin]
                      [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-kerroin])
        (combine-keys +
                      0
                      [:tulokset :kaytettavat-energiamuodot :kertoimella-summa]
                      [:tulokset :kaytettavat-energiamuodot :kaukolampo-kertoimella]
                      [:tulokset :kaytettavat-energiamuodot :sahko-kertoimella]
                      [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-kertoimella]
                      [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-kertoimella]
                      [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-kertoimella])
        (combine-keys +
                      0
                      [:tulokset :kaytettavat-energiamuodot :nettoala-kertoimella-summa]
                      [:tulokset :kaytettavat-energiamuodot :kaukolampo-nettoala-kertoimella]
                      [:tulokset :kaytettavat-energiamuodot :sahko-nettoala-kertoimella]
                      [:tulokset :kaytettavat-energiamuodot :fossiilinen-polttoaine-nettoala-kertoimella]
                      [:tulokset :kaytettavat-energiamuodot :kaukojaahdytys-nettoala-kertoimella]
                      [:tulokset :kaytettavat-energiamuodot :uusiutuva-polttoaine-nettoala-kertoimella])
        (copy-field [:tulokset :kaytettavat-energiamuodot :nettoala-kertoimella-summa]
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
        (assoc-in [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :kevyt-polttooljy-kerroin] 10)
        (assoc-in [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-havu-sekapuu-kerroin] 1300)
        (assoc-in [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-koivu-kerroin] 1700)
        (assoc-in [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :puupelletit-kerroin] 4.7)
        (combine-keys *-ceil
                      nil
                      [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :kevyt-polttooljy-kwh]
                      [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :kevyt-polttooljy]
                      [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :kevyt-polttooljy-kerroin])
        (combine-keys *-ceil
                      nil
                      [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-havu-sekapuu-kwh]
                      [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-havu-sekapuu]
                      [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-havu-sekapuu-kerroin])
        (combine-keys *-ceil
                      nil
                      [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-koivu-kwh]
                      [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-koivu]
                      [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-koivu-kerroin])
        (combine-keys *-ceil
                      nil
                      [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :puupelletit-kwh]
                      [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :puupelletit]
                      [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :puupelletit-kerroin])
        (assoc-div-nettoala [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :kevyt-polttooljy-kwh])
        (assoc-div-nettoala [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-havu-sekapuu-kwh])
        (assoc-div-nettoala [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :pilkkeet-koivu-kwh])
        (assoc-div-nettoala [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :puupelletit-kwh])
        (update-in [:toteutunut-ostoenergiankulutus :ostetut-polttoaineet :vapaa]
                   (fn [vapaat]
                     (if-let [nettoala (-> energiatodistus :lahtotiedot :lammitetty-nettoala)]
                       (mapv (fn [{:keys [maara-vuodessa muunnoskerroin] :as vapaa}]
                               (if (and maara-vuodessa muunnoskerroin)
                                 (let [kwh (* maara-vuodessa muunnoskerroin)]
                                   (assoc vapaa :kwh kwh :kwh-nettoala (div-ceil kwh nettoala)))
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
                      [:toteutunut-ostoenergiankulutus :kaukojaahdytys-vuosikulutus-yhteensa-nettoala]))))

(defn find-complete-energiatodistus
  ([db id]
   (find-complete-energiatodistus* (find-energiatodistus db id)
                                   (find-alakayttotarkoitukset db 2018)))
  ([db whoami id]
   (find-complete-energiatodistus* (find-energiatodistus db whoami id)
                                   (find-alakayttotarkoitukset db 2018))))

(defn find-complete-energiatodistukset-by-laatija [db laatija-id tila-id]
  (let [energiatodistukset (find-energiatodistukset-by-laatija db
                                                               laatija-id
                                                               tila-id)
        alakayttotarkoitukset (find-alakayttotarkoitukset db 2018)]
    (->> (find-energiatodistukset-by-laatija db laatija-id tila-id)
         (map #(find-complete-energiatodistus* % alakayttotarkoitukset)))))
