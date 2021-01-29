(ns solita.etp.schema.energiatodistus-test
  (:require [clojure.test :as t]
            [schema.core :as schema]
            [solita.etp.schema.energiatodistus :as energiatodistus]))

(t/deftest valid-rakennustunnus?-test
 (t/is (nil? (schema/check energiatodistus/Rakennustunnus "1035150826")))
 (t/is (nil? (schema/check energiatodistus/Rakennustunnus "103515074X")))

 (t/is (some? (schema/check energiatodistus/Rakennustunnus "103515074x")))
 (t/is (some? (schema/check energiatodistus/Rakennustunnus "100012345A")))
 (t/is (some? (schema/check energiatodistus/Rakennustunnus nil))))