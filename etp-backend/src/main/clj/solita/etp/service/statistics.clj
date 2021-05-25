(ns solita.etp.service.statistics
  (:require [solita.etp.db :as db]))

;; *** Require sql functions ***
(db/require-queries 'statistics)

(def default-query {:postinumero nil
                    :kunta nil
                    :alakayttotarkoitus-ids []
                    :valmistumisvuosi-min nil
                    :valmistumisvuosi-max nil
                    :lammitetty-nettoala-min nil
                    :lammitetty-nettoala-max nil})

(defn find-e-luokka-counts [db query versio]
  (reduce (fn [acc {:keys [e-luokka count]}]
            (assoc acc e-luokka count))
          {}
          (statistics-db/select-e-luokka-counts db (assoc query
                                                          :versio
                                                          versio))))



(defn find-statistics [db query]
  (let [query (merge default-query query)]
    {:e-luokka-counts {2013 (find-e-luokka-counts db query 2013)
                       2018 (find-e-luokka-counts db query 2018)}}))

(comment
  (find-statistics (user/db) {:postinumero nil
                              :kunta "Tampere"
                              :alakayttotarkoitus-ids ["TT" "RK" "YAT" "T"]
                              :valmistumisvuosi-min 2010
                              :valmistumisvuosi-max 2021
                              :lammitetty-nettoala-max 15000})
  )
