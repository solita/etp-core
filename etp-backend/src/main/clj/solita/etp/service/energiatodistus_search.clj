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
            [solita.etp.schema.public-energiatodistus :as public-energiatodistus-schema]
            [flathead.flatten :as flat]
            [schema.coerce :as coerce]
            [solita.etp.service.json :as json]
            [solita.etp.service.rooli :as rooli-service])
  (:import (schema.core Constrained Predicate EqSchema)
           (clojure.lang ArityException)))

(defn- illegal-argument! [msg]
  (throw (IllegalArgumentException. msg)))

(defn- throw-ex-info [map]
  (throw (ex-info (:message map) map)))

(defn- schema-coercers [schema]
  (cond
    (class? schema) (coerce/coercer! schema json/json-coercions)
    (= schema schema/Int) (coerce/coercer! schema/Int json/json-coercions)
    (xschema/maybe? schema) (schema-coercers (:schema schema))
    (instance? Constrained schema) (schema-coercers (:schema schema))
    (instance? EqSchema schema) (schema-coercers (class (:v schema)))
    (instance? Predicate schema) (illegal-argument! (str "Predicate schema element: " + schema + " is not supported"))
    (map? schema) (m/map-values schema-coercers schema)
    (vector? schema) (mapv schema-coercers schema)
    (coll? schema) (map schema-coercers schema)
    :else (illegal-argument! (str "Unsupported schema element: " schema))))

(def search-schema
  (flat/tree->flat "."
    (deep/deep-merge (schema-coercers energiatodistus-schema/Energiatodistus2013)
                     (schema-coercers energiatodistus-schema/Energiatodistus2018))))

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

(defn- coercer! [field]
  (if-let [coercer (some-> field keyword search-schema)]
    coercer
    (throw-ex-info {:type :unknown-field :field field
                    :message (str "Unknown field: " field)})))

(defn validate-field! [field]
  (coercer! field)
  field)

(defn coerce-value! [field value]
  ((coercer! field) value))

(defn field->sql [field]
  (str "energiatodistus."
       (as-> field $
             (validate-field! $)
             (str/split $ #"\.")
             (update $ 0 abbreviation)
             (map db/snake-case $)
             (str/join "$" $))))

(defn infix-notation [operator field value]
  [(str (field->sql field) " " operator " ?") (coerce-value! field value)])

(defn between-expression [_ field value1 value2]
  [(str (field->sql field) " between ? and ?") (coerce-value! field value1) (coerce-value! field value2)])

(defn is-null-expression [operator field]
  [(str (field->sql field) " is null")])

(def predicates
  {"="  infix-notation
   ">=" infix-notation
   "<=" infix-notation
   ">"  infix-notation
   "<"  infix-notation
   "like"  infix-notation
   "between" between-expression
   "nil?" is-null-expression})

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

(defn whoami->sql [{:keys [id] :as whoami}]
  (cond
    (rooli-service/paakayttaja? whoami)
    ["energiatodistus.tila_id IN (2, 3, 4)"]

    (rooli-service/laatija? whoami)
    ["energiatodistus.laatija_id = ? and energiatodistus.tila_id <> 5" id]

    (rooli-service/public? whoami)
    ["energiatodistus.tila_id = 2"]))

(defn sql-query [whoami {:keys [where sort order limit offset]}]
  (schema/validate [[[(schema/one schema/Str "predicate") schema/Any]]] where)
  (let [[where-sql & where-params] (where->sql where)
        [visibility-sql & visibility-params] (whoami->sql whoami)]
    (concat [(str base-query
                  \newline
                  "where "
                  visibility-sql
                  (when-not (str/blank? where-sql) (format " and (%s) "
                                                           where-sql))
                  (when-not (str/blank? sort)
                    (str \newline "order by " (field->sql sort) " " (or order "asc")))
                  (str \newline "limit " (or limit 100))
                  (when-not (nil? offset) (str \newline "offset " offset)))]
            visibility-params
            where-params)))

(def db-row->public-energiatodistus
  (energiatodistus-service/schema->db-row->energiatodistus
   public-energiatodistus-schema/Energiatodistus))

(defn search [db whoami query]
  (map (comp db-row->public-energiatodistus
          #(assoc % :kommentti nil))
       (jdbc/query db (sql-query whoami query) nil)))
