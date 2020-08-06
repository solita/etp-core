(ns solita.etp.service.energiatodistus-search
  (:require [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [solita.etp.db :as db]
            [solita.etp.service.energiatodistus :as energiatodistus-service]
            [schema.core :as schema]
            [solita.common.map :as m]
            [solita.common.schema :as xschema]
            [flathead.deep :as deep]
            [solita.etp.schema.energiatodistus :as energiatodistus-schema]
            [flathead.flatten :as flat])
  (:import (schema.core Constrained Predicate EqSchema)
           (clojure.lang ArityException)))

(defn- illegal-argument! [msg]
  (throw (IllegalArgumentException. msg)))

(defn- throw-ex-info [map]
  (throw (ex-info (:message map) map)))

(defn- schema-classes [schema]
  (cond
    (class? schema) schema
    (= schema schema/Int) Integer
    (xschema/maybe? schema) (schema-classes (:schema schema))
    (instance? Constrained schema) (schema-classes (:schema schema))
    (instance? EqSchema schema) (class (:v schema))
    (instance? Predicate schema) (illegal-argument! (str "Predicate schema element: " + schema + " is not supported"))
    (map? schema) (m/map-values schema-classes schema)
    (vector? schema) (mapv schema-classes schema)
    (coll? schema) (map schema-classes schema)
    :else (illegal-argument! (str "Unsupported schema element: " schema))))

(def search-schema
  (flat/tree->flat "."
    (deep/deep-merge (schema-classes energiatodistus-schema/Energiatodistus2013)
                     (schema-classes energiatodistus-schema/Energiatodistus2018))))

(def base-query
  "select energiatodistus.*,
          fullname(kayttaja.*) laatija_fullname,
          korvaava_energiatodistus.id as korvaava_energiatodistus_id
   from energiatodistus
     inner join kayttaja on kayttaja.id = energiatodistus.laatija_id
     left join energiatodistus korvaava_energiatodistus on korvaava_energiatodistus.korvattu_energiatodistus_id = energiatodistus.id")

(defn abbreviation [identifier]
  (or (some-> identifier keyword energiatodistus-service/db-abbreviations name)
      identifier))

(defn validate-field! [field]
  (if (nil? (some-> field keyword search-schema))
    (throw-ex-info {:type :unknown-field :field field
                    :message (str "Unknown field: " field)})
    field))

(defn field->sql [field]
  (str "energiatodistus."
       (as-> field $
             (validate-field! $)
             (str/split $ #"\.")
             (update $ 0 abbreviation)
             (map db/snake-case $)
             (str/join "$" $))))

(defn infix-notation [operator field value]
  [(str (field->sql field) " " operator " ?") value])

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

(defn- sql-formatter! [predicate-name]
  (if-let [formatter (predicates predicate-name)]
    formatter
    (throw-ex-info {:type :unknown-predicate :predicate predicate-name
                    :message (str "Unknown predicate: " predicate-name)})))

(defn predicate-expression->sql [expression]
  (let [predicate (first expression)]
    (try
      (apply (sql-formatter! predicate) expression)
      (catch ArityException _
        (throw-ex-info {:type :invalid-arguments :predicate predicate
                        :message (str "Wrong number of arguments: " (rest expression)
                                      " for predicate: " predicate)})))))

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

(defn query->sql [{:keys [where sort order limit offset]}]
  (schema/validate [[[(schema/one schema/Str "predicate") schema/Any]]] where)

  (let [[where-sql & params] (where->sql where)]
    (cons (str base-query
               (when-not (blank? where-sql) (str \newline "where " where-sql))
               (when-not (blank? sort)
                 (str \newline "order by " (field->sql sort) " " (or order "asc")))
               (str \newline "limit " (or limit 100))
               (when-not (nil? offset) (str \newline "offset " offset)))
          params)))

(defn search [db query]
  (map energiatodistus-service/db-row->energiatodistus
       (jdbc/query db (query->sql query) nil)))

