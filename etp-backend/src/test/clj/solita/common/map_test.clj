(ns solita.common.map-test
  (:require [clojure.test :as t]
            [solita.common.map :as solita-map]))

(def test-map-1 {:a {:b {:d :foobar}
                     :c :baz}})
(def test-map-2 {:a [:foo :bar {:d :foobarbaz}]
                 :b {:c :foobar}})

(t/deftest paths-test
  (t/is (= (solita-map/paths nil) []))
  (t/is (= (solita-map/paths []) []))
  (t/is (= (sort (solita-map/paths test-map-1)) [[:a :c] [:a :b :d]]))
  (t/is (= (sort (solita-map/paths test-map-2)) [[:a 0] [:a 1] [:b :c] [:a 2 :d]])))
