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

(t/deftest dissoc-in-test
  (let [test-map {:a 1 :b {:c 2 :d {:e 4}}}]
    (t/is (= nil (solita-map/dissoc-in nil [:a])))
    (t/is (= {} (solita-map/dissoc-in {} [:a])))
    (t/is (= {} (solita-map/dissoc-in {:a 1} [:a])))
    (t/is (= {:a 1} (solita-map/dissoc-in test-map [:b])))
    (t/is (= {:a 1 :b {:d {:e 4}}} (solita-map/dissoc-in test-map [:b :c])))
    (t/is (= {:a 1 :b {:c 2}} (solita-map/dissoc-in test-map [:b :d])))
    (t/is (= {:a 1 :b {:c 2 :d {}}} (solita-map/dissoc-in test-map [:b :d :e])))))
