(ns solita.etp.service.laatija
  (:require [clojure.java.jdbc :as jdbc]
            [solita.common.map :as map]
            [solita.etp.db :as db]
            [solita.etp.schema.laatija :as laatija-schema]
            [solita.etp.service.json :as json]
            [schema.coerce :as coerce]))

; *** Require sql functions ***
(db/require-queries 'laatija)

; *** Conversions from database data types ***
(def coerce-laatija (coerce/coercer laatija-schema/Laatija json/json-coercions))

(defn find-all-laatijat [db]
  (map (comp coerce-laatija json/merge-data) (laatija-db/select-laatijat db)))

(defn find-laatija [db id]
  (first (map (comp coerce-laatija json/merge-data) (laatija-db/select-laatija db {:id id}))))

(defn find-laatija-yritykset [db id]
  (map :yritys-id (laatija-db/select-laatija-yritykset db {:id id})))

(defn attach-laatija-yritys [db laatija-id yritys-id]
  (laatija-db/insert-laatija-yritys! db (map/bindings->map laatija-id yritys-id)))

(defn find-laatija-with-henkilotunnus [db henkilotunnus]
  (first (map (comp coerce-laatija json/merge-data) (laatija-db/select-laatija-with-henkilotunnus db {:henkilotunnus henkilotunnus}))))

(defn add-or-update-existing-laatijat! [db laatijat]
  (jdbc/with-db-transaction
    [db db]
    (mapv (fn [{:keys [henkilotunnus] :as laatija}]
            ; TODO: use upsert instead
            (if-let [{:keys [id] :as existing-laatija} (find-laatija-with-henkilotunnus db henkilotunnus)]
              (let [laatija-update (merge existing-laatija (select-keys laatija [:patevyys :patevyys-voimassaoloaika]))]
                (laatija-db/update-laatija! db {:id   id
                                                :data (json/write-value-as-string laatija-update)})
                id)
              (:id (laatija-db/insert-laatija<! db {:data (json/write-value-as-string laatija)})))) laatijat)))

;;
;; Pätevyydet
;;

(def patevyystasot [{:id 1 :label-fi "Perustaso" :label-sv "Basnivå"}
                    {:id 2 :label-fi "Ylempi taso" :label-sv "Högre nivå"}])

(defn find-patevyystasot [] patevyystasot)

