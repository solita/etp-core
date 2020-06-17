(ns solita.etp.service.e-luokka
  (:require [clojure.core.match :as match]
            [solita.etp.service.energiatodistus :as energiatodistus-service]))

(defn e-luokka-from-e-luku-and-nettoala [e-luku nettoala limits default-luokka]
  (or (some (fn [[limit kerroin e-luokka]]
              (if (<= e-luku (->> nettoala (* kerroin) (- limit) int))
                e-luokka))
            limits)
      default-luokka))

(defn e-luokka-from-e-luku [e-luku limits default-luokka]
  (or (some (fn [[limit e-luokka]]
              (if (<= e-luku limit)
                e-luokka))
            limits)
      default-luokka))

(defn pienet-asuinrakennukset-120-2013 [e-luku _]
  (e-luokka-from-e-luku
   e-luku
   [[94 "A"] [164 "B"] [204 "C"] [284 "D"] [414 "E"] [484 "F"]]
   "G"))

(defn pienet-asuinrakennukset-120-150-2013 [e-luku nettoala]
  (e-luokka-from-e-luku-and-nettoala
   e-luku
   nettoala
   [[150 0.47 "A"] [320 1.3 "B"] [372 1.4 "C"] [452 1.4 "D"] [582 1.4 "E"]
    [652 1.4 "F"]]
   "G"))

(defn pienet-asuinrakennukset-150-600-2013-2018 [e-luku nettoala]
  (e-luokka-from-e-luku-and-nettoala
   e-luku
   nettoala
   [[83 0.02 "A"] [131 0.04 "B"] [173 0.07 "C"] [253 0.07 "D"] [383 0.07 "E"]
    [453 0.07 "F"]]
   "G"))

(defn pienet-asuinrakennukset-600-2013-2018 [e-luku _]
  (e-luokka-from-e-luku
   e-luku
   [[70 "A"] [106 "B"] [130 "C"] [210 "D"] [340 "E"] [410 "F"]]
   "G"))

(defn pienet-asuinrakennukset-50-150-2018 [e-luku nettoala]
  (e-luokka-from-e-luku-and-nettoala
   e-luku
   nettoala
   [[110 0.2 "A"] [215 0.6 "B"] [252 0.6 "C"] [332 0.6 "D"] [462 0.6 "E"]
    [532 0.6 "F"]]
   "G"))

(defn rivitalot-2013-2018 [e-luku _]
  (e-luokka-from-e-luku
   e-luku
   [[80 "A"] [110 "B"] [150 "C"] [210 "D"] [340 "E"] [410 "F"]]
   "G"))

(defn asuinkerrostalot-2013-2018 [e-luku _]
  (e-luokka-from-e-luku
   e-luku
   [[75 "A"] [100 "B"] [130 "C"] [160 "D"] [190 "E"] [240 "F"]]
   "G"))

(defn toimistorakennukset-2013-2018 [e-luku _]
  (e-luokka-from-e-luku
   e-luku
   [[80 "A"] [120 "B"] [170 "C"] [200 "D"] [240 "E"] [300 "F"]]
   "G"))

(defn liikerakennukset-2013-2018 [e-luku _]
  (e-luokka-from-e-luku
   e-luku
   [[90 "A"] [170 "B"] [240 "C"] [280 "D"] [340 "E"] [390 "F"]]
   "G"))

(defn majoitusliikerakennukset-2013-2018 [e-luku _]
  (e-luokka-from-e-luku
   e-luku
   [[90 "A"] [170 "B"] [240 "C"] [280 "D"] [340 "E"] [450 "F"]]
   "G"))

(defn opetusrakennukset-2013-2018 [e-luku _]
  (e-luokka-from-e-luku
   e-luku
   [[90 "A"] [130 "B"] [170 "C"] [230 "D"] [300 "E"] [360 "F"]]
   "G"))

(defn liikuntahallit-2013-2018 [e-luku _]
  (e-luokka-from-e-luku
   e-luku
   [[90 "A"] [130 "B"] [170 "C"] [190 "D"] [240 "E"] [280 "F"]]
   "G"))

(defn sairaalat-2013-2018 [e-luku _]
  (e-luokka-from-e-luku
   e-luku
   [[150 "A"] [350 "B"] [450 "C"] [550 "D"] [650 "E"] [800 "F"]]
   "G"))

(defn muut-2018 [e-luku _]
  (e-luokka-from-e-luku
   e-luku
   [[90 "A"] [130 "B"] [170 "C"] [190 "D"] [240 "E"] [280 "F"]]
   "G"))

(defn e-luokka-f [versio kayttotarkoitus-id alakayttotarkoitus-id nettoala]
  (match/match [versio kayttotarkoitus-id alakayttotarkoitus-id nettoala]
               [2013 1 _ (_ :guard #(<= % 120))] pienet-asuinrakennukset-120-2013
               [2013 1 _ (_ :guard #(<= % 150))] pienet-asuinrakennukset-120-150-2013
               [2013 1 _ (_ :guard #(<= % 600))] pienet-asuinrakennukset-150-600-2013-2018
               [2013 1 _ (_ :guard #(> % 600))] pienet-asuinrakennukset-600-2013-2018

               ;; For 2018, rivitalot is part of käyttötarkoitusluokka 1 for some reason
               ;; and for 2013 it is a separate käyttötarkoitusluokka
               [2013 2 _ _] rivitalot-2013-2018
               [2018 1 "RT" _] rivitalot-2013-2018
               [2018 1 "AK2" _] rivitalot-2013-2018

               ;; Even though the table is for building between 50-150 squares,
               ;; this table is used for even smaller buildings
               [2018 1 _ (_ :guard #(<= % 150))] pienet-asuinrakennukset-50-150-2018
               [2018 1 _ (_ :guard #(<= % 600))] pienet-asuinrakennukset-150-600-2013-2018
               [2018 1 _ (_ :guard #(> % 600))] pienet-asuinrakennukset-600-2013-2018

               ;; Luckily the rest of the tables are pretty much the same for 2013
               ;; and 2018

               [2013 3 _ _] asuinkerrostalot-2013-2018
               [2018 2 _ _] asuinkerrostalot-2013-2018

               [2013 4 _ _] toimistorakennukset-2013-2018
               [2018 3 _ _] toimistorakennukset-2013-2018

               [2013 5 _ _] liikerakennukset-2013-2018
               [2018 4 _ _] liikerakennukset-2013-2018

               [2013 6 _ _] majoitusliikerakennukset-2013-2018
               [2018 5 _ _] majoitusliikerakennukset-2013-2018

               [2013 7 _ _] opetusrakennukset-2013-2018
               [2018 6 _ _] opetusrakennukset-2013-2018

               [2013 8 _ _] liikuntahallit-2013-2018
               [2018 7 _ _] liikuntahallit-2013-2018

               [2013 9 _ _] sairaalat-2013-2018
               [2018 8 _ _] sairaalat-2013-2018

               ;; TODO 2013 10 and 11?

               [2018 9 _ _] muut-2018

               :else (constantly "?")))

(defn find-e-luokka [db versio alakayttotarkoitus-id nettoala e-luku]
  (let [kayttotarkoitus-id (energiatodistus-service/find-kayttotarkoitus-id-by-alakayttotarkoitus-id
                            db
                            versio
                            alakayttotarkoitus-id)
        f (e-luokka-f versio kayttotarkoitus-id alakayttotarkoitus-id nettoala)]
    {:e-luokka (f e-luku nettoala)}))
