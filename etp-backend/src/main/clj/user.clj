(ns user
  (:require [integrant.repl :refer [clear go halt prep init reset reset-all]]
            [clojure.test :as t]))

(integrant.repl/set-prep!
 (fn []
   (require 'solita.etp.system)
   ((resolve 'solita.etp.system/config))))

(defn db
  ([] (-> integrant.repl.state/system :solita.etp/db))
  ([kayttaja-id] (assoc (db) :application-name (str kayttaja-id "@core.etp.test"))))

(defn aws-s3-client []
  (-> integrant.repl.state/system :solita.etp/aws-s3-client))

(defn run-test [var-name]
  (t/test-vars [var-name]))

(defn matching-test [test-searches]
  (if (empty? test-searches) (fn [_] true)
      (fn [test]
        (some #(re-find % (str test)) test-searches))))

(defn run-tests [test-searches]
  (require 'eftest.runner)
  (->> ((resolve 'eftest.runner/find-tests) "src/test")
       (filter (matching-test test-searches))
       ((resolve 'eftest.runner/run-tests))))

(defn run-tests-and-exit! []
  (let [{:keys [fail error]} (run-tests (map re-pattern *command-line-args*))]
    (System/exit (if (and (zero? fail) (zero? error)) 0 1))))
