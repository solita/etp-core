(ns solita.etp.service.energiatodistus-validation
  (:require [clojure.string :as str]
            [solita.etp.exception :as exception]
            [solita.etp.service.laatimisvaihe :as laatimisvaihe]
            [solita.etp.service.kielisyys :as kielisyys]))

(def required-condition
  {"perustiedot.rakennustunnus" (complement laatimisvaihe/rakennuslupa?)
   "perustiedot.havainnointikaynti" laatimisvaihe/olemassaoleva-rakennus?
   "perustiedot.keskeiset-suositukset-fi"
   (every-pred laatimisvaihe/olemassaoleva-rakennus? kielisyys/fi?)
   "perustiedot.keskeiset-suositukset-sv"
   (every-pred laatimisvaihe/olemassaoleva-rakennus? kielisyys/sv?)})

(defn localized-property-condition [property]
  (cond
    (str/ends-with? property "-fi") kielisyys/fi?
    (str/ends-with? property "-sv") kielisyys/sv?
    :else nil))

(def path
  (comp
    (partial map keyword)
    #(str/split % #"\.")))

(defn assoc-last [v new-value]
  (assoc v (dec (count v)) new-value))

(defn u-property-condition [property]
  (when (str/ends-with? property ".U")
    (let [u-path (vec (path property))
          ala-path (assoc-last u-path :ala)]
      #(> (get-in % ala-path 0) 0))))

(defn required-constraints
  "Required constraint is a pair of predicate and a required property.
  The property is required if predicate is true for particular energiatodistus."
  [required-properties]
  (map #(vector (or (get required-condition %)
                    (localized-property-condition %)
                    (u-property-condition %)
                    (constantly true)) %)
       required-properties))

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
