(ns solita.common.map)

(defn map-keys [f m] (into {} (map (fn [[k, v]] [(f k) v]) m)))

(defn map-values [f m] (into {} (map (fn [[k, v]] [k (f v)]) m)))

(defmacro bindings->map [& bindings]
  (into {} (map (fn [s] [(keyword (name s)) s]) bindings)))
