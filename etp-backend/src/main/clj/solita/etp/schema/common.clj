(ns solita.etp.schema.common
  (:require [clojure.string :as str]
            [schema.core :as schema]))

(defn not-contains-keys [object schema]
  (every? #(not (contains? object %)) (keys schema)))

(def Key schema/Int)
(def Id {:id Key})

(def IdAndWarnings (assoc Id :warnings [{:property schema/Str
                                         :value schema/Num
                                         :min schema/Num
                                         :max schema/Num}]))

(defn StringBase [max]
  (schema/constrained schema/Str #(<= 1 (count %) max) (str "[1, " max "]")))

(def String8 (StringBase 8))
(def String12 (StringBase 12))
(def String30 (StringBase 30))
(def String35 (StringBase 35))
(def String50 (StringBase 75))
(def String60 (StringBase 60))
(def String75 (StringBase 75))
(def String100 (StringBase 100))
(def String150 (StringBase 150))
(def String200 (StringBase 200))
(def String500 (StringBase 500))
(def String1000 (StringBase 1000))
(def String1500 (StringBase 1500))
(def String2500 (StringBase 2500))
(def String6300 (StringBase 6300))

(def Year
  (schema/constrained schema/Int #(<= 0 % 9999) "Year"))

(defn- LimitedNum
  ([number-type mininclusive maxinclusive name]
   (schema/constrained number-type
                       #(<= mininclusive % maxinclusive) name)))

(def Num1
  (LimitedNum schema/Num 0 1 "[0, 1]"))

(def NonNegative
  (LimitedNum schema/Num 0 9999999999 "[0, max]"))

(def IntNonNegative
  (LimitedNum schema/Int 0 9999999999 "[0, max]"))

(def Luokittelu (merge Id {:label-fi schema/Str
                           :label-sv schema/Str
                           :valid schema/Bool}))

(def Date java.time.LocalDate)
(def DateInterval {:start Date
                   :end Date})
(def Instant java.time.Instant)

(defn henkilotunnus-checksum [s]
  (try
    (->> (mod (. Integer parseInt s) 31)
         (nth [\0 \1 \2 \3 \4 \5 \6 \7 \8 \9
               \A \B \C \D \E \F \H \J \K \L
               \M \N \P \R \S \T \U \V \W \X \Y]))
    (catch NumberFormatException _ false)))

(defn valid-henkilotunnus? [s]
  (try
    (let [date-part         (subs s 0 6)
          century-sign      (nth s 6)
          individual-number (subs s 7 10)
          checksum          (last s)]
      (and (= 11 (count s))
           (contains? #{\+ \- \A} century-sign)
           (= checksum (henkilotunnus-checksum (str date-part individual-number)))))
    (catch StringIndexOutOfBoundsException _ false)))

(def Henkilotunnus (schema/constrained schema/Str valid-henkilotunnus?
                                       "henkilotunnus"))

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

(def Ytunnus (schema/constrained schema/Str valid-ytunnus?
                                 "y-tunnus"))

(def ConstraintError
  { :type schema/Keyword
    :constraint schema/Keyword})

(defn valid-ovt-tunnus? [s]
  (if (re-find #"^0037\d{8,13}$" s)
    (let [ytunnus (str (subs s 4 11) "-" (subs s 11 12))]
      (valid-ytunnus? ytunnus))
    false))

(def OVTtunnus (schema/constrained schema/Str valid-ovt-tunnus?
                                   "ovt-tunnus"))

(def iban-char-map (zipmap (map char (range (int \a) (inc (int \z))))
                           (range 10 36) ))

(defn valid-iban? [s]
  (try
    (let [country (subs s 0 2)
          checksum (subs s 2 4)
          bban (subs s 4)]
      (= (mod (->> (str bban country checksum)
                   str/lower-case
                   (map #(or (get iban-char-map %) %))
                   (apply str)
                   bigint)
              97)
         1))
    (catch Exception e
      false)))

(def IBAN (schema/constrained schema/Str valid-iban? "iban"))

(defn valid-te-ovt-tunnus? [s]
  (and (-> s str/lower-case (str/starts-with? "te"))
       (-> s (subs 2) valid-ovt-tunnus?)))

(def TEOVTtunnus (schema/constrained schema/Str
                                     valid-te-ovt-tunnus?
                                     "te-ovt-tunnus"))

(defn valid-verkkolaskuosoite? [s]
  (or (valid-ovt-tunnus? s)
      (valid-iban? s)
      (valid-te-ovt-tunnus? s)))

(def Verkkolaskuosoite (schema/constrained schema/Str
                                           valid-verkkolaskuosoite?
                                           "verkkolaskuosoite"))

(defn valid-url? [s]
  (re-matches #"^https?:\/\/(www\.)?[-\p{L}0-9@:%._\+~#=]{1,256}\.[-\p{L}0-9()]{1,6}\b([-\p{L}0-9()@:%_\+.~#?&//=]*$)"
              s))

(def Url (schema/constrained schema/Str valid-url?))
