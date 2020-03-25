(ns solita.etp.service.laatija
  (:require [schema.coerce :as coerce]
            [solita.common.map :as map]
            [solita.etp.db :as db]
            [solita.etp.service.json :as json]
            [solita.etp.schema.laatija :as laatija-schema]))

;; *** Require sql functions ***
(db/require-queries 'laatija)

;; *** Conversions from database data types ***
(def coerce-laatija (coerce/coercer laatija-schema/Laatija json/json-coercions))

(defn find-laatija-with-kayttaja-id [db kayttaja-id]
  (->> {:kayttaja kayttaja-id}
       (laatija-db/select-laatija-with-kayttaja db)
       (map coerce-laatija)
       first))

(defn find-laatija-with-henkilotunnus [db henkilotunnus]
  (->> {:henkilotunnus henkilotunnus}
       (laatija-db/select-laatija-with-henkilotunnus db)
       (map coerce-laatija)
       first))

(defn add-laatija! [db laatija]
  (:id (laatija-db/insert-laatija<! db laatija)))

(defn update-laatija-with-kayttaja-id! [db kayttaja-id laatija]
  (laatija-db/update-laatija! db (assoc laatija :kayttaja kayttaja-id)))

(defn find-laatija-yritykset [db id]
  (map :yritys-id (laatija-db/select-laatija-yritykset db {:id id})))

(defn attach-laatija-yritys [db laatija-id yritys-id]
  (laatija-db/insert-laatija-yritys! db (map/bindings->map laatija-id yritys-id)))

(defn detach-laatija-yritys [db laatija-id yritys-id]
  (laatija-db/delete-laatija-yritys! db (map/bindings->map laatija-id yritys-id)))

;;
;; Pätevyydet
;;

(def patevyystasot [{:id 1 :label-fi "Perustaso" :label-sv "Basnivå"}
                    {:id 2 :label-fi "Ylempi taso" :label-sv "Högre nivå"}])

(defn find-patevyystasot [] patevyystasot)
