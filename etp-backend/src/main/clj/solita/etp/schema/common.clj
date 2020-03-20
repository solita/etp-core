(ns solita.etp.schema.common
  (:require [clojure.string :as str]
            [schema.core :as schema]))

(def Key schema/Int)
(def Id {:id Key})

(def Luokittelu (merge Id {:label-fi schema/Str
                           :label-sv schema/Str
                           (schema/optional-key :deleted) schema/Bool}))

(def Date java.time.LocalDate)
(def DateInterval {:start Date
                   :end Date})

(defn henkilotunnus-checksum [s]
  (try
    (->> (mod (. Integer parseInt s) 31)
         (nth [\0 \1 \2 \3 \4 \5 \6 \7 \8 \9
               \a \b \c \d \e \f \h \j \k \l
               \m \n \p \r \s \t \u \v \w \x \y]))
    (catch NumberFormatException _ false)))

(defn valid-henkilotunnus? [s]
  (let [s (str/lower-case s)
        date-part (subs s 0 6)
        century-sign (nth s 6)
        individual-number (subs s 7 10)
        checksum (last s)]
    (and (= 11 (count s))
         (contains? #{\+ \- \a} century-sign)
         (= checksum (henkilotunnus-checksum (str date-part individual-number))))))

(def Henkilotunnus (schema/constrained schema/Str valid-henkilotunnus?))

(defn ytunnus-checksum [ytunnus]
  (let [digits (map #(-> % str Integer/parseInt) (subs ytunnus 0 7))
        sum (reduce + (map * digits [7 9 10 5 8 4 2]))
        remainder (rem sum 11)]
    (if (= remainder 0) 0 (- 11 remainder))))

(defn valid-ytunnus? [ytunnus]
  (let [checksum (ytunnus-checksum ytunnus)]
    (and (= 9 (count ytunnus))
         (not= 10 checksum)
         (= \- (get ytunnus 7))
         (= checksum (Integer/parseInt (str (get ytunnus 8)))))))

(def Ytunnus (schema/constrained schema/Str valid-ytunnus?))

(def ConstraintError
  { :type schema/Keyword
    :constraint schema/Keyword})
