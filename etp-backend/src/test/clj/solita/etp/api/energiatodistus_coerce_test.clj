(ns solita.etp.api.energiatodistus-coerce-test
  (:require [solita.etp.api.energiatodistus :as et-api]
            [schema.core :as schema]
            [clojure.test :as t]))

(defn- coerce [schema value]
  ((et-api/missing-maybe-values-coercer schema) value))

(t/deftest missing-maybe-values-coercer-test
  (t/is (= (coerce {:a schema/Str} {:a "a"}) {:a "a"}))
  (t/is (= (coerce {:a (schema/maybe schema/Str)} {}) {:a nil}))
  (t/is (= (coerce {:a [{:b (schema/maybe schema/Str)}]} {:a []})
           {:a []}))
  (t/is (= (coerce {:a [{:b (schema/maybe schema/Str)}]} {:a [{}]})
           {:a [{:b nil}]}))
  (t/is (= (coerce {:a [{:b (schema/maybe schema/Str)}]} {:a [{} {}]})
           {:a [{:b nil} {:b nil}]})))
