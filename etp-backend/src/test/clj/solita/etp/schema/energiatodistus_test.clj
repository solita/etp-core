(ns solita.etp.schema.energiatodistus-test
  (:require [clojure.test :as t]
            [schema.core :as schema]
            [solita.etp.schema.energiatodistus :as energiatodistus]))

(t/deftest valid-rakennustunnus?-test
 (t/is (nil? (schema/check energiatodistus/Rakennustunnus "1000000009")))
 (t/is (nil? (schema/check energiatodistus/Rakennustunnus "100000000A")))
 (t/is (nil? (schema/check energiatodistus/Rakennustunnus "100000000a")))
 (t/is (nil? (schema/check energiatodistus/Rakennustunnus "100012345A")))

 (t/is (some? (schema/check energiatodistus/Rakennustunnus "2000000009")))
 (t/is (some? (schema/check energiatodistus/Rakennustunnus nil))))