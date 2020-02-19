(ns solita.etp.schema.common
  (:require [clojure.string :as str]
            [schema.core :as schema]))

(def Key schema/Int)
(def Id {:id Key})

(def Luokittelu (merge Id {:label-fi schema/Str
                           :label-se schema/Str
                           (schema/optional-key :deleted) schema/Bool}))

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
