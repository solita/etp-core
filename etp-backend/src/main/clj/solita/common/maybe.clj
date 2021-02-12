(ns solita.common.maybe)

(defn map* [fn optional] (when (some? optional) (fn optional)))
