(ns solita.etp.service.energiatodistus-search
  (:require [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [solita.etp.db :as db]))

(def base-query "SELECT energiatodistus.id FROM energiatodistus WHERE ")

(defn k->sql [k]
  (if (keyword? k)
    (->> k name (format "'%s'"))
    (str k)))

(defn path->sql [path]
  (let [path (map k->sql path)]
    (str "data->" (str/join "->" (butlast path)) "->>" (last path))))

(defn query-part->sql [[op path v]]
  (let [cast (when (number? v) "::numeric")]
    (str (when cast "(")
         (path->sql path)
         (when cast ")")
         cast
         " "
         op
         " ?")))

(defn or-query->sql-and-params [or-query]
  {:sql (str/join " OR " (map query-part->sql or-query))
   :params (mapv #(nth % 2) or-query)})

(defn and-query->sql-and-params [and-query]
  (let [or-query-sqls-and-params (map or-query->sql-and-params and-query)]
    {:sql (->> or-query-sqls-and-params
               (map #(->> % :sql (format "(%s)")))
               (str/join " AND "))
     :params (apply concat (map :params or-query-sqls-and-params))}))

(defn query->sql [{:keys [where sort order limit offset] :or {order ""}}]
  (if (empty? where)
    nil
    (let [{:keys [sql params]} (and-query->sql-and-params where)]
      (concat [(str base-query
                    sql
                    (when sort (format " ORDER BY %s %s" (path->sql sort) order))
                    (when limit (str " LIMIT " limit))
                    (when offset (str " OFFSET " offset)))]
              params))))

(defn search [db query]
  (jdbc/query db (query->sql query) nil))
