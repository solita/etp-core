(ns solita.etp.service.e-luokka)

(defn limit [nettoala x kerroin]
  (->> nettoala (* kerroin) (- x) int))

(defn e-luokka-from-e-luku-and-nettoala [e-luku nettoala limits default-luokka]
  (or (some (fn [[x kerroin e-luokka]]
              (if (<= e-luku (limit nettoala x kerroin))
                e-luokka))
            limits)
      default-luokka))

(defn pienet-asuinrakennukset-50-150-2018 [e-luku nettoala]
  (e-luokka-from-e-luku-and-nettoala
   e-luku
   nettoala
   [[110 0.2 "A"] [215 0.6 "B"] [252 0.6 "C"] [332 0.6 "D"] [462 0.6 "E"]
    [532 0.6 "F"]]
   "G"))

(defn find-e-luokka [db versio alakayttotarkoitusluokka nettoala e-luku]
  {:e-luokka "A"})
