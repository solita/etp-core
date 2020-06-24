(ns solita.common.schema
  (:require [schema.core :as schema]
            [schema.coerce :as coerce]
            [schema-tools.core :as schema-tools]
            [clojure.walk :as walk]
            [schema-tools.coerce :as schema-tools-coerce])
  (:import (schema.core Maybe)))

(defn maybe? [schema]
  (instance? Maybe schema))

(defn optional-key-for-maybe [schema]
  (let [convert
        (fn [[key value :as entry]]
          (if (and (keyword? key) (maybe? value))
            [(schema/optional-key key) value]
            entry))]
    (walk/postwalk
      #(if (map-entry? %) (convert %) %)
      schema)))

(defn- default-value-for-maybe [schema]
  (let [convert
        (fn [[key value :as entry]]
          (if (and (keyword? key) (maybe? value))
            [key (schema-tools/default value nil)]
            entry))]
    (walk/postwalk
      #(if (map-entry? %) (convert %) %)
      schema)))

(defn missing-maybe-values-coercer [schema]
  (schema-tools-coerce/coercer
    (default-value-for-maybe schema)
    schema-tools-coerce/default-key-matcher))

(def parse-big-decimal
  (coerce/safe #(if (string? %) (BigDecimal. ^String %) %)))
