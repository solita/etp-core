(ns solita.etp.db
  (:require [integrant.core :as ig]
            [hikari-cp.core :as hikari]
            [jeesql.core :as jeesql]
            [jeesql.generate :as jeesql-generate]
            [clojure.string :as str])
  (:import (org.postgresql.util PSQLException)))

(defmethod ig/init-key :solita.etp/db
  [_ opts]
  {:datasource (hikari/make-datasource opts)})

(defmethod ig/halt-key! :solita.etp/db
  [_ {:keys [datasource]}]
  (hikari/close-datasource datasource))

(defn with-db-exception-translation [db-function args]
  (try
    (apply db-function args)
    (catch PSQLException psqle
      (let [error (.getServerErrorMessage psqle)]
        (throw
          (case (.getSQLState error)
            "23505"
            (ex-info
              (.getMessage psqle)
              {:type       :unique-violation
               :constraint (keyword (str/replace (.getConstraint error) "_" "-"))}
              psqle)
            psqle))))))

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
