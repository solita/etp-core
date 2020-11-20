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
            [solita.etp.schema.geo :as geo-schema]
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


(defn schemas->search-schema [& schemas]
  (->> schemas
       (map schema-coercers)
       (apply deep/deep-merge)
       (flat/tree->flat ".")))

(def admin-search-schema (schemas->search-schema
                    {:energiatodistus energiatodistus-schema/Energiatodistus2013}
                    {:energiatodistus energiatodistus-schema/Energiatodistus2018}
                    geo-schema/Search))

(def public-search-schema (schemas->search-schema
                           public-energiatodistus-schema/Energiatodistus2013
                           public-energiatodistus-schema/Energiatodistus2018
                           geo-schema/Search))

(def base-query
  "SELECT energiatodistus.*,
          fullname(kayttaja.*) laatija_fullname,
          korvaava_energiatodistus.id AS korvaava_energiatodistus_id
   FROM energiatodistus
   INNER JOIN kayttaja ON kayttaja.id = energiatodistus.laatija_id
   LEFT JOIN energiatodistus korvaava_energiatodistus
     ON korvaava_energiatodistus.korvattu_energiatodistus_id = energiatodistus.id
   LEFT JOIN postinumero ON postinumero.id = energiatodistus.pt$postinumero
   LEFT JOIN kunta ON kunta.id = postinumero.kunta_id
   LEFT JOIN toimintaalue ON toimintaalue.id = kunta.toimintaalue_id")

(defn abbreviation [identifier]
  (or (some-> identifier keyword energiatodistus-service/db-abbreviations name)
      identifier))

(defn- coercer! [field search-schema]
  (if-let [coercer (some-> field keyword search-schema)]
    coercer
    (throw-ex-info {:type :unknown-field :field field
                    :message (str "Unknown field: " field)})))

(defn validate-field! [field search-schema]
  (coercer! field search-schema)
  field)

(defn coerce-value! [field value search-schema]
  ((coercer! field search-schema) value))

(defn field->sql [field search-schema]
  (validate-field! field search-schema)
  (let [[table & field-parts] (str/split field #"\.")]
    (str table "." (as-> field-parts $
                     (vec $)
                     (update $ 0 abbreviation)
                     (map db/snake-case $)
                     (str/join "$" $)))))

(defn infix-notation [operator field value search-schema]
  [(str (field->sql field search-schema) " " operator " ?")
   (coerce-value! field value search-schema)])

(defn between-expression [_ field value1 value2 search-schema]
  [(str (field->sql field search-schema) " between ? and ?")
   (coerce-value! field value1 search-schema)
   (coerce-value! field value2 search-schema)])

(defn is-null-expression [operator field search-schema]
  [(str (field->sql field search-schema) " is null")])

(defn in-expression [_ field values search-schema]
  [(str (field->sql field search-schema) " = any (?)")
   (mapv #(coerce-value! field % search-schema) values)])

(def predicates
  {"="  infix-notation
   ">=" infix-notation
   "<=" infix-notation
   ">"  infix-notation
   "<"  infix-notation
   "like"  infix-notation
   "between" between-expression
   "nil?" is-null-expression
   "in" in-expression})

(defn- sql-formatter! [predicate-name]
  (if-let [formatter (predicates predicate-name)]
    formatter
    (throw-ex-info {:type :unknown-predicate :predicate predicate-name
                    :message (str "Unknown predicate: " predicate-name)})))

(defn predicate-expression->sql [search-schema expression]
  (let [predicate (first expression)]
    (try
      (apply (sql-formatter! predicate) (concat expression [search-schema]))
      (catch ArityException _
        (throw-ex-info {:type :invalid-arguments :predicate predicate
                        :message (str "Wrong number of arguments: " (rest expression)
                                      " for predicate: " predicate)})))))

(defn expression-seq->sql [logic-operator expression->sql expressions]
  (let [sql-expressions (map expression->sql expressions)]
    (cons (str/join (format " %s " logic-operator) (map first sql-expressions))
          (mapcat rest sql-expressions))))

(defn where->sql [where search-schema]
  (expression-seq->sql
    "or"
    #(expression-seq->sql
       "and" (partial predicate-expression->sql search-schema) %)
    where))

(defn keyword->sql [keyword]
  (when (-> keyword str/blank? not)
    (concat
     ["postinumero.id::text = ? OR kunta.label_fi ILIKE ? OR
       kunta.label_sv ILIKE ? OR toimintaalue.label_fi ILIKE ? OR
       toimintaalue.label_sv ILIKE ?"]
     [keyword]
     (repeat 4 (str keyword "%")))))

(defn whoami->sql [{:keys [id] :as whoami}]
  (cond
    (rooli-service/paakayttaja? whoami)
    ["energiatodistus.tila_id IN (2, 3, 4)"]

    (rooli-service/laatija? whoami)
    ["energiatodistus.laatija_id = ? AND energiatodistus.tila_id <> 5" id]

    (rooli-service/public? whoami)
    ["energiatodistus.tila_id = 2 AND
     ((energiatodistus.versio = 2013 AND
       energiatodistus.pt$kayttotarkoitus NOT IN ('YAT', 'KAT', 'MEP', 'MAEP'))
      OR
      (energiatodistus.versio = 2018 AND
       energiatodistus.pt$kayttotarkoitus NOT IN ('YAT', 'KAT', 'KREP')))"]))

(defn search-schema [whoami]
  (if (rooli-service/public? whoami)
    public-search-schema
    admin-search-schema))

(defn sql-query [whoami {:keys [sort order limit offset where keyword]}]
  (schema/validate [[[(schema/one schema/Str "predicate") schema/Any]]] where)
  (let [search-schema (search-schema whoami)
        [where-sql & where-params] (where->sql where search-schema)
        [keyword-sql & keyword-params] (keyword->sql keyword)
        [visibility-sql & visibility-params] (whoami->sql whoami)]
    (concat [(str base-query
                  \newline
                  "where "
                  visibility-sql
                  (when-not (str/blank? where-sql)
                    (format " and (%s) " where-sql))
                  (when-not (str/blank? keyword-sql)
                    (format " and (%s) " keyword-sql))
                  (when-not (str/blank? sort)
                    (str \newline "order by " (field->sql sort search-schema) " " (or order "asc")))
                  (str \newline "limit " (or limit 100))
                  (when-not (nil? offset) (str \newline "offset " offset)))]
            visibility-params
            where-params
            keyword-params)))

(def db-row->public-energiatodistus
  (energiatodistus-service/schema->db-row->energiatodistus
   public-energiatodistus-schema/Energiatodistus))

(defn search [db whoami query]
  (map (comp db-row->public-energiatodistus
          #(assoc % :kommentti nil))
       (jdbc/query db (sql-query whoami query) nil)))
