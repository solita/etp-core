(ns solita.common.maybe)

(defn map* [fn optional] (if (some? optional) (fn optional) nil))
