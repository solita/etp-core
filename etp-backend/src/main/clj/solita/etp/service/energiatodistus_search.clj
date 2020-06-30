(ns solita.etp.service.energiatodistus-search
  (:require [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [solita.etp.db :as db]
            [solita.etp.service.energiatodistus :as energiatodistus-service]
            [schema.core :as schema]))

(def base-query
  "select energiatodistus.*,
          fullname(kayttaja.*) laatija_fullname
   from energiatodistus
     inner join kayttaja on kayttaja.id = energiatodistus.laatija_id")

(defn abbreviation [identifier]
  (or (some-> identifier keyword energiatodistus-service/db-abbreviations name)
      identifier))

(defn field->sql [field]
  (str "energiatodistus."
       (as-> field $
             (str/split $ #"\.")
             (update $ 0 abbreviation)
             (map db/snake-case $)
             (str/join "$" $))))

(defn infix-notation [operator field value]
  [(str (field->sql field) operator "?") value])

(defn between-expression [_ field value1 value2]
  [(str field " between ? and ?") value1 value2])

(def predicates
  {"="  infix-notation
   ">=" infix-notation
   "<=" infix-notation
   ">"  infix-notation
   "<"  infix-notation
   "like"  infix-notation
   "between" between-expression})

(defn predicate-expression->sql [expression]
  (apply (predicates (first expression)) expression))

(defn expression-seq->sql [logic-operator expression->sql expressions]
  (let [sql-expressions (map expression->sql expressions)]
    (cons (str/join (format " %s " logic-operator) (map first sql-expressions))
          (mapcat rest sql-expressions))))

(defn where->sql [where]
  (expression-seq->sql
    "or"
    #(expression-seq->sql "and" predicate-expression->sql %)
    where))

(def blank? (some-fn nil? empty?))

(defn query->sql [{:keys [where sort order limit offset] :or {order "asc"}}]
  (schema/validate [[[(schema/one schema/Str "predicate") schema/Any]]] where)

  (let [[where-sql & params] (where->sql where)]
    (cons (str base-query
               (when-not (blank? where-sql) (str \newline "where " where-sql))
               (when-not (blank? sort)      (str \newline "order by " (field->sql sort) " " order))
               (when-not (blank? limit)     (str \newline "limit " limit))
               (when-not (blank? offset)    (str \newline "offset " offset)))
          params)))

(defn search [db query]
  (map energiatodistus-service/db-row->energiatodistus
       (jdbc/query db (query->sql query) nil)))

