(ns user
  (:require [integrant.repl :refer [clear go halt prep init reset reset-all]]))

(integrant.repl/set-prep!
 (fn []
   (require 'solita.etp.system)
   ((resolve 'solita.etp.system/config))))

(defn db []
  (-> integrant.repl.state/system :solita.etp/db))

(defn run-tests []
  (require 'eftest.runner)
  (-> ((resolve 'eftest.runner/find-tests) "src/test")
      ((resolve 'eftest.runner/run-tests))))
