(ns solita.etp.schema.common-test
  (:require [clojure.test :as t]
            [schema.core :as schema]
            [solita.etp.schema.common :as common]))

(t/deftest valid-hetu?-test
  (t/is (some? (schema/check common/Hetu "131052B308T")))
  (t/is (some? (schema/check common/Hetu "131053-308T")))
  (t/is (some? (schema/check common/Hetu "0131053-308T")))
  (t/is (nil? (schema/check common/Hetu "131052-308T")))
  (t/is (some? (schema/check common/Hetu "130200X892S")))
  (t/is (some? (schema/check common/Hetu "130200A891S")))
  (t/is (some? (schema/check common/Hetu "1A0200A892S")))
  (t/is (nil? (schema/check common/Hetu "130200A892S"))))
