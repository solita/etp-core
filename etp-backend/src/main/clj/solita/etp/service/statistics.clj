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

(defn find-e-luku-statistics [db query versio]
  (first (statistics-db/select-e-luku-statistics db (assoc query
                                                           :versio
                                                           versio))))

(defn find-common-averages [db query]
  (first (statistics-db/select-common-averages db query)))

(defn find-statistics [db query]
  (let [query (merge default-query query)]
    {:e-luokka-counts {2013 (find-e-luokka-counts db query 2013)
                       2018 (find-e-luokka-counts db query 2018)}
     :e-luku-statistics {2013 (find-e-luku-statistics db query 2013)
                         2018 (find-e-luku-statistics db query 2018)}
     :common-averages (find-common-averages db query)}))

(comment
  (find-statistics (user/db) {:postinumero nil
                              :kunta "Tampere"
                              :alakayttotarkoitus-ids ["TT" "RK" "YAT" "T"]
                              :valmistumisvuosi-min 2010
                              :valmistumisvuosi-max 2021
                              :lammitetty-nettoala-max 15000})
  )
