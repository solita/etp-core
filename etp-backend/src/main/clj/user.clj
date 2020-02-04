(ns user
  (:require [integrant.repl :refer [clear go halt prep init reset reset-all]]))

(integrant.repl/set-prep!
 (fn []
   (require 'solita.etp.system)
   ((resolve 'solita.etp.system/load-config) "dev.edn")))
