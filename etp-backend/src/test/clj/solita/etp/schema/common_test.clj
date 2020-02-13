(ns solita.etp.schema.common-test
  (:require [clojure.test :as t]
            [solita.etp.schema.common :as common]))

(t/deftest valid-hetu?-test
  (t/is (false? (common/valid-hetu? "131052B308T")))
  (t/is (false? (common/valid-hetu? "131053-308T")))
  (t/is (false? (common/valid-hetu? "0131053-308T")))
  (t/is (true? (common/valid-hetu? "131052-308T")))
  (t/is (false? (common/valid-hetu? "130200X892S")))
  (t/is (false? (common/valid-hetu? "130200A891S")))
  (t/is (false? (common/valid-hetu? "1A0200A892S")))
  (t/is (true? (common/valid-hetu? "130200A892S"))))
