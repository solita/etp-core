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

(def min-sample-size 5)

(defn sufficient-sample-size? [e-luokka-counts]
  (->> e-luokka-counts vals (reduce +) (<= min-sample-size)))

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

(defn find-luokittelu-counts [db query versio]
  (reduce (fn [acc {:keys [lammitysmuoto-id ilmanvaihtotyyppi-id count]}]
            (cond-> acc
              lammitysmuoto-id
              (assoc-in [:lammitysmuoto lammitysmuoto-id] count)

              ilmanvaihtotyyppi-id
              (assoc-in [:ilmanvaihto ilmanvaihtotyyppi-id] count)))
          {}
          (statistics-db/select-luokittelu-counts db (assoc query
                                                            :versio
                                                            versio))))

(defn find-uusiutuvat-omavaraisenergiat-counts [db query versio]
  (first (statistics-db/select-uusiutuvat-omavaraisenergiat-counts
          db
          (assoc query :versio versio))))

(defn future-when [f pred]
  (future (when pred (f))))

(defn find-statistics [db query]
  (let [query (merge default-query query)
        e-luokka-counts-2013 (future-when
                              #(find-e-luokka-counts db query 2013)
                              true)
        e-luokka-counts-2018 (future-when
                              #(find-e-luokka-counts db query 2018)
                              true)
        return-2013? (sufficient-sample-size? @e-luokka-counts-2013)
        return-2018? (sufficient-sample-size? @e-luokka-counts-2018)
        e-luku-statistics-2013 (future-when
                                #(find-e-luku-statistics db query 2013)
                                return-2013?)
        e-luku-statistics-2018 (future-when
                                #(find-e-luku-statistics db query 2018)
                                return-2018?)
        common-averages (future-when
                         #(find-common-averages db query)
                         (or return-2013? return-2018?))
        luokittelu-counts-2018 (future-when
                                #(find-luokittelu-counts db query 2018)
                                return-2018?)
        uusiutuvat-omavaraisenergiat-counts-2018
        (future-when
         #(find-uusiutuvat-omavaraisenergiat-counts db query 2018)
         return-2018?)]
    {:e-luokka-counts {2013 (when return-2013? @e-luokka-counts-2013)
                       2018 (when return-2018? @e-luokka-counts-2018)}
     :e-luku-statistics {2013 @e-luku-statistics-2013
                         2018 @e-luku-statistics-2018}
     :common-averages @common-averages
     :luokittelu-counts {2018 @luokittelu-counts-2018}
     :uusiutuvat-omavaraisenergiat-counts
     {2018 @uusiutuvat-omavaraisenergiat-counts-2018}}))
