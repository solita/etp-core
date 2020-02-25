(ns solita.etp.schema.common-test
  (:require [clojure.test :as t]
            [schema.core :as schema]
            [solita.etp.schema.common :as common]))

(t/deftest valid-henkilotunnus?-test
  (t/is (some? (schema/check common/Henkilotunnus "131052B308T")))
  (t/is (some? (schema/check common/Henkilotunnus "131053-308T")))
  (t/is (some? (schema/check common/Henkilotunnus "0131053-308T")))
  (t/is (nil? (schema/check common/Henkilotunnus "131052-308T")))
  (t/is (some? (schema/check common/Henkilotunnus "130200X892S")))
  (t/is (some? (schema/check common/Henkilotunnus "130200A891S")))
  (t/is (some? (schema/check common/Henkilotunnus "1A0200A892S")))
  (t/is (nil? (schema/check common/Henkilotunnus "130200A892S"))))


(t/deftest valid-ytunnus?-test
  (t/is (nil? (schema/check common/Ytunnus "1234567-1")))
  (t/is (some? (schema/check common/Ytunnus "1234567-2")))
  (t/is (nil? (schema/check common/Ytunnus "1060155-5")))
  (t/is (some? (schema/check common/Ytunnus "1060155-6")))
  (t/is (some? (schema/check common/Ytunnus "1060155-7")))
  (t/is (nil? (schema/check common/Ytunnus "0000001-9")))

  (t/is (some? (schema/check common/Ytunnus "a060155-7")))
  (t/is (some? (schema/check common/Ytunnus "aaaaaaa-b"))))