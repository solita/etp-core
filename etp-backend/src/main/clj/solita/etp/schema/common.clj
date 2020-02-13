(ns solita.etp.schema.common
  (:require [clojure.string :as str]
            [schema.core :as schema]))

(def Id {:id schema/Num})

(def Postiosoite
  {:katuosoite                        schema/Str
   (schema/optional-key :postilokero) schema/Str
   :postinumero                       schema/Str
   :postitoimipaikka                  schema/Str
   :maa                               schema/Str})

(defn hetu-checksum [s]
  (try
    (->> (mod (. Integer parseInt s) 31)
         (nth [\0 \1 \2 \3 \4 \5 \6 \7 \8 \9
               \a \b \c \d \e \f \h \j \k \l
               \m \n \p \r \s \t \u \v \w \x \y]))
    (catch NumberFormatException _ false)))

(defn valid-hetu? [s]
  (let [s (str/lower-case s)
        date-part (subs s 0 6)
        century-sign (nth s 6)
        individual-number (subs s 7 10)
        checksum (last s)]
    (and (= 11 (count s))
         (contains? #{\+ \- \a} century-sign)
         (= checksum (hetu-checksum (str date-part individual-number))))))

(def Hetu (schema/pred hetu?))
