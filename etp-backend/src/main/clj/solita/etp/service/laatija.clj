(ns solita.etp.service.laatija
  (:require [clojure.java.jdbc :as jdbc]
            [schema.coerce :as coerce]
            [solita.common.map :as map]
            [solita.etp.db :as db]
            [solita.etp.service.json :as json]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.schema.laatija :as laatija-schema]))

;; *** Require sql functions ***
(db/require-queries 'laatija)

;; *** Conversions from database data types ***
(def coerce-laatija (coerce/coercer laatija-schema/Laatija json/json-coercions))

(defn find-laatija-with-kayttaja-id
  ([db kayttaja-id]
   (->> {:kayttaja kayttaja-id}
        (laatija-db/select-laatija-with-kayttaja db)
        (map coerce-laatija)
        first))
  ([db whoami kayttaja-id]
   (when (or (= kayttaja-id (:id whoami))
             (rooli-service/laatija-maintainer? whoami))
     (find-laatija-with-kayttaja-id db kayttaja-id))))

(defn find-laatija-with-henkilotunnus [db henkilotunnus]
  (->> {:henkilotunnus henkilotunnus}
       (laatija-db/select-laatija-with-henkilotunnus db)
       (map coerce-laatija)
       first))

(defn add-laatija! [db laatija]
  (-> (jdbc/insert! db :laatija laatija) first :id))

(defn update-laatija-with-kayttaja-id! [db kayttaja-id laatija]
  (jdbc/update! db :laatija laatija ["kayttaja = ?" kayttaja-id]))

(defn find-laatija-yritykset [db whoami id]
  (when (or (= id (:laatija whoami))
            (rooli-service/laatija-maintainer? whoami))
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
