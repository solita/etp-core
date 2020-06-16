(ns solita.etp.service.e-luokka)

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

(defn find-e-luokka [db versio alakayttotarkoitusluokka nettoala e-luku]
  {:e-luokka "A"})
