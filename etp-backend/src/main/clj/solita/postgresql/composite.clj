(ns solita.postgresql.composite
  (:require [clojure.data.csv :as csv]
            [flathead.deep :as deep]
            [clojure.string :as str])
  (:import (org.postgresql.util PGobject)))

(defn- backslash-escape [txt character]
  (str/replace txt character (str "\\" character)))

(defn escape-value [value]
  (some-> value
          (backslash-escape "\\")
          (backslash-escape ",")
          (backslash-escape "(" )
          (backslash-escape ")" )
          (backslash-escape "\"" )))

(defn unescape-value [txt]
  (some-> txt
          (str/replace "\\\\" "\\")
          #_(str/replace "\"\"" "\"")))

(defn composite-literal-writer [composite-type-keys]
  (comp
    #(str "(" % ")")
    (partial str/join ",")
    #_(partial map #(if (empty? %) "\"\"" %))
    (partial map escape-value)
    (apply juxt composite-type-keys)))

(defn composite-literal-parser [composite-type-keys]
  (comp
    #(zipmap composite-type-keys %)
    (partial map #(if (empty? %) nil %))
    (partial map unescape-value)
    first
    csv/read-csv
    #(subs % 1 (dec (count %)))))

(defn parse-composite-type-literals [composite-type-schema db-object]
  (deep/deep-merge-with
    (fn [[keys value]]
      (let [read #((composite-literal-parser keys) (.getValue %))]
        (cond
          (instance? PGobject value) (read value)
          (seqable? value) (map read value)
          :else (throw (IllegalArgumentException.
                         (str "Unsupported postgresql object value: " value))))))
    composite-type-schema db-object))

(defn write-composite-type-literals [composite-type-schema db-object]
  (deep/deep-merge-with
    (fn [[keys value]]
      (let [write (composite-literal-writer keys)]
        (cond
          (map? value) (write value)
          (seqable? value) (mapv write value)
          :else (throw (IllegalArgumentException.
                         (str "Unsupported composite type value: " value))))))
    composite-type-schema db-object))
