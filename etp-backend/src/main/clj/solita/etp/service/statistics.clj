(ns solita.etp.service.statistics
  (:require [solita.etp.db :as db]))

;; *** Require sql functions ***
(db/require-queries 'statistics)

(def default-query {:keyword nil
                    :alakayttotarkoitus-ids []
                    :valmistumisvuosi-min nil
                    :valmistumisvuosi-max nil
                    :lammitetty-nettoala-min nil
                    :lammitetty-nettoala-max nil})

(def min-sample-size 5)

(defn sufficient-sample-size? [counts]
  (->> counts :e-luokka vals (reduce +) (<= min-sample-size)))

(defn find-counts [db query versio]
  (reduce (fn [acc {:keys [e-luokka lammitysmuoto-id ilmanvaihtotyyppi-id
                          count]}]
            (cond-> acc
              e-luokka
              (assoc-in [:e-luokka e-luokka] count)

              lammitysmuoto-id
              (assoc-in [:lammitysmuoto lammitysmuoto-id] count)

              ilmanvaihtotyyppi-id
              (assoc-in [:ilmanvaihto ilmanvaihtotyyppi-id] count)))
          {}
          (statistics-db/select-counts db (assoc query :versio versio))))

(defn find-e-luku-statistics [db query versio]
  (first (statistics-db/select-e-luku-statistics db (assoc query
                                                           :versio
                                                           versio))))

(defn find-common-averages [db query]
  (first (statistics-db/select-common-averages db query)))

(defn find-uusiutuvat-omavaraisenergiat-counts [db query versio]
  (first (statistics-db/select-uusiutuvat-omavaraisenergiat-counts
          db
          (assoc query :versio versio))))

(defn future-when [f pred]
  (future (when pred (f))))

(defn find-statistics [db query]
  (let [query (merge default-query query)
        counts-2013 (future-when #(find-counts db query 2013) true)
        counts-2018 (future-when #(find-counts db query 2018) true)
        return-2013? (sufficient-sample-size? @counts-2013)
        return-2018? (sufficient-sample-size? @counts-2018)
        e-luku-statistics-2013 (future-when
                                #(find-e-luku-statistics db query 2013)
                                return-2013?)
        e-luku-statistics-2018 (future-when
                                #(find-e-luku-statistics db query 2018)
                                return-2018?)
        common-averages (future-when
                         #(find-common-averages db query)
                         (or return-2013? return-2018?))
        uusiutuvat-omavaraisenergiat-counts-2018
        (future-when
         #(find-uusiutuvat-omavaraisenergiat-counts db query 2018)
         return-2018?)]
    {:counts {2013 (when return-2013? @counts-2013)
              2018 (when return-2018? @counts-2018)}
     :e-luku-statistics {2013 @e-luku-statistics-2013
                         2018 @e-luku-statistics-2018}
     :common-averages @common-averages
     :uusiutuvat-omavaraisenergiat-counts
     {2018 @uusiutuvat-omavaraisenergiat-counts-2018}}))
