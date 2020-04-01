(ns solita.etp.service.laatija
  (:require [schema.coerce :as coerce]
            [solita.common.map :as map]
            [solita.etp.db :as db]
            [solita.etp.service.json :as json]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.schema.laatija :as laatija-schema]))

;; *** Require sql functions ***
(db/require-queries 'laatija)

;; *** Conversions from database data types ***
(def coerce-laatija (coerce/coercer laatija-schema/Laatija json/json-coercions))

(defn find-laatija-with-kayttaja-id [db whoami kayttaja-id]
  (when (or (= kayttaja-id (:id whoami))
            (rooli-service/more-than-laatija? whoami))
    (->> {:kayttaja kayttaja-id}
         (laatija-db/select-laatija-with-kayttaja db)
         (map coerce-laatija)
         first)))

(defn find-laatija-with-henkilotunnus [db henkilotunnus]
  (->> {:henkilotunnus henkilotunnus}
       (laatija-db/select-laatija-with-henkilotunnus db)
       (map coerce-laatija)
       first))

(defn add-laatija!
  ([db laatija]
   (:id (laatija-db/insert-laatija<! db laatija)))
  ([db whoami laatija]
   (when (rooli-service/more-than-laatija? whoami)
     (add-laatija! db laatija))))

(defn update-laatija-with-kayttaja-id!
  ([db kayttaja-id laatija]
   (laatija-db/update-laatija! db (assoc laatija :kayttaja kayttaja-id)))
  ([db whoami kayttaja-id laatija]
   (when (or (= kayttaja-id (:id whoami))
             (rooli-service/paakayttaja? whoami))
     (update-laatija-with-kayttaja-id! db kayttaja-id laatija))))

(defn find-laatija-yritykset [db whoami id]
  (when (or (= id (:laatija whoami))
            (rooli-service/more-than-laatija? whoami))
    (map :yritys-id (laatija-db/select-laatija-yritykset db {:id id}))))

(defn attach-laatija-yritys [db whoami laatija-id yritys-id]
  (when (= laatija-id (:laatija whoami))
    (laatija-db/insert-laatija-yritys!
     db
     (map/bindings->map laatija-id yritys-id))))

(defn detach-laatija-yritys [db whoami laatija-id yritys-id]
  (when (= laatija-id (:laatija whoami))
    (laatija-db/delete-laatija-yritys!
     db
     (map/bindings->map laatija-id yritys-id))))

;;
;; Pätevyydet
;;

(def patevyystasot [{:id 1 :label-fi "Perustaso" :label-sv "Basnivå"}
                    {:id 2 :label-fi "Ylempi taso" :label-sv "Högre nivå"}])

(defn find-patevyystasot [] patevyystasot)
