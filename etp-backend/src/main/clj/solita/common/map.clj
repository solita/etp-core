(ns solita.common.map)

(defn map-keys [f m] (into {} (map (fn [[k, v]] [(f k) v]) m)))

(defn map-values [f m] (into {} (map (fn [[k, v]] [k (f v)]) m)))

(defn submap? [m1 m2] (= m1 (select-keys m2 (keys m1))))

(defmacro bindings->map [& bindings]
  (into {} (map (fn [s] [(keyword (name s)) s]) bindings)))
