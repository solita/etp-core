(ns solita.etp.service.kielisyys)

(def kielisyys [{:id 0 :label-fi "Suomi" :label-sv "Finska" :valid true}
                {:id 1 :label-fi "Ruotsi" :label-sv "Svenska" :valid true}
                {:id 2 :label-fi "Kaksikielinen" :label-sv "Tvåspråkig" :valid true}])

(defn find-kielisyys [] kielisyys)

(def ^:private kieli-keys
  [:fi,
   :sv,
   :bilingual])

(defn kieli-key [kieli-id] (nth kieli-keys kieli-id))

(defn- kielisyys? [kieli-id energiatodistus]
  (= (-> energiatodistus :perustiedot :kieli)
     kieli-id))

(def only-fi? (partial kielisyys? 0))
(def only-sv? (partial kielisyys? 1))
(def bilingual? (partial kielisyys? 2))

(def fi? (some-fn only-fi? bilingual?))
(def sv? (some-fn only-sv? bilingual?))