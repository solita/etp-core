(ns solita.etp.db
  (:require [clojure.java.jdbc :as jdbc]
            [integrant.core :as ig]
            [hikari-cp.core :as hikari]
            [jeesql.core :as jeesql]
            [jeesql.generate :as jeesql-generate]
            [clojure.string :as str])
  (:import (org.postgresql.util PSQLException ServerErrorMessage)))

(defmethod ig/init-key :solita.etp/db
  [_ opts]
  {:datasource (hikari/make-datasource opts)})

(defmethod ig/halt-key! :solita.etp/db
  [_ {:keys [datasource]}]
  (hikari/close-datasource datasource))

(defn constraint [^ServerErrorMessage error]
  (keyword (str/replace (.getConstraint error) "_" "-")))

(defn translatePSQLException [^PSQLException psqle]
  (let [error (.getServerErrorMessage psqle)]
    (case (.getSQLState error)
      "23505"
      (ex-info
        (.getMessage psqle)
        {:type       :unique-violation
         :constraint (constraint error)}
        psqle)
      "23503"
      (ex-info
        (.getMessage psqle)
        {:type       :foreign-key-violation
         :constraint (constraint error)}
        psqle)
      psqle)))

(defn with-db-exception-translation [db-function args]
  (try
    (apply db-function args)
    (catch PSQLException psqle
      (throw (translatePSQLException psqle)))
    (catch Exception e
      (throw
        (let [cause (.getCause e)]
          (if (instance? PSQLException cause)
            (translatePSQLException cause) e))))))

(defn- generate-query-fn [original-generate-query-fn ns query query-options]
  (let [db-function (original-generate-query-fn ns query query-options)]
    (with-meta
      (fn [& args]
        (with-db-exception-translation db-function args))
      (meta db-function))))

(defn require-queries
  ([name] (require-queries name {}))
  ([name options]
   (let [db-namespace (symbol (str "solita.etp.db." name))
         original-generate-query-fn jeesql-generate/generate-query-fn]
     (binding [*ns* (create-ns db-namespace)]
       (with-redefs [jeesql-generate/generate-query-fn
                     (partial generate-query-fn original-generate-query-fn)]
         (jeesql/defqueries (str "solita/etp/db/" name ".sql") options)))
     (alias (symbol (str name "-db")) db-namespace))))

;;
;; Protocol extensions
;;

(extend-protocol jdbc/IResultSetReadColumn
  java.sql.Date
  (result-set-read-column [x _ _]
    (.toLocalDate x))

  java.sql.Timestamp
  (result-set-read-column [x _ _]
    (.toInstant x))

  org.postgresql.jdbc.PgArray
  (result-set-read-column [x _ _]
    (-> x .getArray vec)))

(extend-protocol clojure.java.jdbc/ISQLParameter
  clojure.lang.IPersistentVector
  (set-parameter [v ^java.sql.PreparedStatement stmt ^long i]
    (let [conn (.getConnection stmt)
          meta (.getParameterMetaData stmt)
          type-name (.getParameterTypeName meta i)]
      (if-let [elem-type (when (= (first type-name) \_) (apply str (rest type-name)))]
        (.setObject stmt i (.createArrayOf conn elem-type (to-array v)))
        (.setObject stmt i v)))))
