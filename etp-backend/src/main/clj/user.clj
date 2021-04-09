(ns user
  (:require [integrant.repl :refer [clear go halt prep init reset reset-all]]
            [clojure.test :as t]
            [clj-async-profiler.core :as prof]))

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

(defn run-tests []
  (require 'eftest.runner)
  (-> ((resolve 'eftest.runner/find-tests) "src/test")
      ((resolve 'eftest.runner/run-tests))))

(defn run-tests-and-exit! []
  (let [{:keys [fail error]} (run-tests)]
    (System/exit (if (and (zero? fail) (zero? error)) 0 1))))

(intern 'user
        (with-meta 'profile {:macro true})
        @#'prof/profile)

(def start-profiling prof/start)
(def stop-profiling prof/stop)
(def clear-profiling-results prof/clear-results)

(defn serve-flamegraphs
  ([] (serve-flamegraphs 8080))
  ([port]
   (println (str "Serving flamegraphs in http://localhost:" port))
   (prof/serve-files port)))
