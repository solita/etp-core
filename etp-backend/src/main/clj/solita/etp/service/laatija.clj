(ns solita.etp.service.laatija
  (:require [clojure.set :as set]
            [clojure.java.jdbc :as jdbc]
            [buddy.hashers :as hashers]
            [solita.common.map :as map]
            [solita.etp.exception :as exception]
            [solita.etp.db :as db]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.service.yritys :as yritys-service]
            [solita.etp.service.luokittelu :as luokittelu-service]
            [solita.etp.schema.laatija :as laatija-schema]))

;; *** Require sql functions ***
(db/require-queries 'laatija)

(defn public-laatija [{:keys [login voimassa laatimiskielto julkinenpuhelin
                              julkinenemail julkinenwwwosoite julkinenosoite]
                       :as laatija}]
  (when (and voimassa (not laatimiskielto) (not (nil? login)))
    (select-keys laatija
                 (cond-> laatija-schema/always-public-kayttaja-laatija-ks
                   julkinenpuhelin (conj :puhelin)
                   julkinenemail (conj :email)
                   julkinenwwwosoite (conj :wwwosoite)
                   julkinenosoite (conj :jakeluosoite
                                        :postinumero
                                        :postitoimipaikka
                                        :maa)))))

(defn find-all-laatijat [db whoami]
  (if (rooli-service/laatija? whoami)
    (exception/throw-forbidden! "Laatija can't list all laatijas.")
    (->> (laatija-db/select-laatijat db)
         (keep (fn [laatija]
                 (cond (or (rooli-service/paakayttaja? whoami)
                           (rooli-service/laskuttaja? whoami))
                       laatija

                       (rooli-service/patevyydentoteaja? whoami)
                       (update laatija :henkilotunnus #(subs % 0 6))

                       (rooli-service/public? whoami)
                       (public-laatija laatija)))))))

(defn assert-read-access! [whoami id]
  (when-not (or (= id (:id whoami))
                (rooli-service/patevyydentoteaja? whoami)
                (rooli-service/paakayttaja? whoami)
                (rooli-service/laskuttaja? whoami))
    (exception/throw-forbidden!
      (str "User " (:id whoami)
           " is not allowed to access laatija: "
           id " information."))))

(defn find-laatija-by-id
  ([db id]
   (->> {:id id}
        (laatija-db/select-laatija-by-id db)
        first))
  ([db whoami id]
   (assert-read-access! whoami id)
   (find-laatija-by-id db id)))

(defn find-laatija-with-henkilotunnus [db henkilotunnus]
  (->> {:henkilotunnus henkilotunnus}
       (laatija-db/select-laatija-with-henkilotunnus db)
       first))

(def db-keymap {:muuttoimintaalueet :muut_toimintaalueet
                :julkinenpuhelin :julkinen_puhelin
                :julkinenemail :julkinen_email
                :julkinenosoite :julkinen_osoite
                :julkinenwwwosoite :julkinen_wwwosoite})

(defn add-laatija! [db laatija]
  (->> (set/rename-keys laatija db-keymap)
       (jdbc/insert! db :laatija)
       first
       :id))

(defn- api-key-hash [laatija]
  (if-let [api-key (:api-key laatija)]
    (assoc laatija :api-key-hash
                   (hashers/derive api-key {:alg :bcrypt+sha512}))
    laatija))

(defn update-laatija-by-id! [db id laatija]
  (jdbc/update! db
                :laatija
                (-> laatija
                    (set/rename-keys db-keymap)
                    api-key-hash
                    (dissoc :api-key))
                ["id = ?" id] db/default-opts))

(defn validate-laatija-patevyys! [db user-id]
  (if-let [laatija (find-laatija-by-id db user-id)]
    (do
      (when-not (:voimassa laatija)
        (exception/throw-ex-info!
          {:type :patevyys-expired
           :paattymisaika (:voimassaolo-paattymisaika laatija)
           :message (str "Laatija: " user-id " pätevyys has expired.")}))
      (when (:laatimiskielto laatija)
        (exception/throw-ex-info!
          :laatimiskielto (str "Laatija: " user-id " has laatimiskielto."))))
    (exception/throw-ex-info!
      :not-laatija (str "User: " user-id " is not a laatija."))))

(defn find-laatija-yritykset
  ([db whoami id]
    (assert-read-access! whoami id)
    (laatija-db/select-laatija-yritykset db {:id id})))

(defn find-laatija-laskutusosoitteet
  ([db id]
   (laatija-db/select-laatija-laskutusosoitteet db {:id id}))
  ([db whoami id]
   (assert-read-access! whoami id)
   (find-laatija-laskutusosoitteet db id)))

(defn add-laatija-yritys! [db whoami laatija-id yritys-id]
  (if (= laatija-id (:id whoami))
    (do
      (laatija-db/insert-laatija-yritys!
       db
       (map/bindings->map laatija-id yritys-id))
      nil)
    (exception/throw-forbidden!)))

(defn detach-laatija-yritys! [db whoami laatija-id yritys-id]
  (if (or (rooli-service/paakayttaja? whoami)
          (= laatija-id (:id whoami))
          (yritys-service/laatija-in-yritys? db (:id whoami) yritys-id))
    (laatija-db/delete-laatija-yritys!
     db
     (map/bindings->map laatija-id yritys-id))
    (exception/throw-forbidden!)))

;;
;; Pätevyydet
;;

(defn find-patevyystasot [db] (luokittelu-service/find-patevyystasot db))
