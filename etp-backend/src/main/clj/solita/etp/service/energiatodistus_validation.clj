(ns solita.etp.service.energiatodistus-validation
  (:require [clojure.string :as str]
            [solita.etp.exception :as exception]
            [solita.etp.service.laatimisvaihe :as laatimisvaihe]))

(def required-condition
  {"perustiedot.rakennustunnus" (complement laatimisvaihe/rakennuslupa?)
   "perustiedot.havainnointikaynti" laatimisvaihe/olemassaoleva-rakennus?})

(defn required-constraints
  "Required constraint is a pair of predicate and a required property.
  The property is required if predicate is true for particular energiatodistus."
  [required-properties]
  (map #(vector (or (get required-condition %) (constantly true)) %)
       required-properties))

(def path
  (comp
    (partial map keyword)
    #(str/split % #"\.")))

(defn missing-properties [required-constraints energiatodistus]
  (->> required-constraints
       (filter (fn [[predicate property]]
                 (and (predicate energiatodistus)
                      (nil? (get-in energiatodistus (path property))))))
       (map second)))

(defn validate-required!
  ([required-constraints current update]
    (let [missing-current (vec (missing-properties required-constraints current))
          constraints (filter #(not (contains? missing-current (second %)))
                              required-constraints)]
      (validate-required! constraints update)))

  ([required-constraints energiatodistus]
    (let [missing (missing-properties required-constraints energiatodistus)]
      (when-not (empty? missing)
        (exception/throw-ex-info!
          {:type :missing-value
           :missing missing
           :message (str "Required value is missing in properties: "
                         (str/join ", " missing))})))))
