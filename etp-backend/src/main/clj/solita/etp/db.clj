(ns solita.etp.db
  (:require [integrant.core :as ig]
            [hikari-cp.core :as hikari]
            [jeesql.core :as jeesql]))

(defmethod ig/init-key :solita.etp/db
  [_ opts]
  (-> opts
      (select-keys [:server-name :database-name])
      (assoc :datasource (hikari/make-datasource opts))))

(defmethod ig/halt-key! :solita.etp/db
  [_ {:keys [datasource]}]
  (hikari/close-datasource datasource))

(defn require-queries
  ([name] (require-queries name {}))
  ([name options]
   (let [db-namespace (symbol (str "solita.etp.db." name))]
     (binding [*ns* (create-ns db-namespace)]
       (jeesql/defqueries (str "solita/etp/db/" name ".sql") options))
     (alias (symbol (str name "-db")) db-namespace))))