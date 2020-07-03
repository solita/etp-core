(ns solita.etp.service.laatija
  (:require [clojure.set :as set]
            [clojure.java.jdbc :as jdbc]
            [schema.coerce :as coerce]
            [solita.common.map :as map]
            [solita.etp.exception :as exception]
            [solita.etp.db :as db]
            [solita.etp.service.json :as json]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.schema.laatija :as laatija-schema]))

;; *** Require sql functions ***
(db/require-queries 'laatija)

;; *** Conversions from database data types ***
(def coerce-laatija (coerce/coercer laatija-schema/Laatija json/json-coercions))

(defn find-all-laatijat [db whoami]
  (->> (laatija-db/select-laatijat db)
       (map (fn [laatija]
              (cond (rooli-service/paakayttaja? whoami) laatija
                    (rooli-service/patevyydentoteaja? whoami) (update laatija :henkilotunnus #(subs % 0 6))
                    :else  (dissoc laatija :henkilotunnus))))))

(defn find-laatija-by-id
  ([db id]
   (->> {:id id}
        (laatija-db/select-laatija-by-id db)
        (map coerce-laatija)
        first))
  ([db whoami id]
   (if (or (= id (:id whoami))
             (rooli-service/laatija-maintainer? whoami))
     (find-laatija-by-id db id)
     (exception/throw-forbidden!))))

(defn find-laatija-with-henkilotunnus [db henkilotunnus]
  (->> {:henkilotunnus henkilotunnus}
       (laatija-db/select-laatija-with-henkilotunnus db)
       (map coerce-laatija)
       first))

(def db-keymap {:muuttoimintaalueet :muut_toimintaalueet
                :julkinenpuhelin :julkinen_puhelin
                :julkinenemail :julkinen_email
                :julkinenosoite :julkinen_osoite
                :vastaanottajan-tarkenne :vastaanottajan_tarkenne})

(defn add-laatija! [db laatija]
  (->> (set/rename-keys laatija db-keymap)
       (jdbc/insert! db :laatija)
       first
       :id))

(defn update-laatija-by-id! [db id laatija]
  (jdbc/update! db
                :laatija
                (set/rename-keys laatija db-keymap)
                ["id = ?" id]))

(defn find-laatija-yritykset [db whoami id]
  (if (or (= id (:id whoami))
          (rooli-service/laatija-maintainer? whoami))
    (map :yritys-id (laatija-db/select-laatija-yritykset db {:id id}))
    (exception/throw-forbidden!)))

(defn attach-laatija-yritys [db whoami laatija-id yritys-id]
  (if (= laatija-id (:id whoami))
    (do
      (laatija-db/insert-laatija-yritys!
       db
       (map/bindings->map laatija-id yritys-id))
      nil)
    (exception/throw-forbidden!)))

(defn detach-laatija-yritys [db whoami laatija-id yritys-id]
  (if (= laatija-id (:id whoami))
    (laatija-db/delete-laatija-yritys!
     db
     (map/bindings->map laatija-id yritys-id))
    (exception/throw-forbidden!)))

;;
;; Pätevyydet
;;

(def patevyystasot [{:id 1 :label-fi "Perustaso" :label-sv "Basnivå"}
                    {:id 2 :label-fi "Ylempi taso" :label-sv "Högre nivå"}])

(defn find-patevyystasot [] patevyystasot)
