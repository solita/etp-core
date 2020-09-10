(ns solita.etp.service.laatimisvaihe)

(def ^:private laatimisvaiheet
  [{:id 0 :label-fi "Rakennuslupa" :label-sv "Bygglov" :valid true}
   {:id 1 :label-fi "Käyttöönotto" :label-sv "Införandet" :valid true}
   {:id 2 :label-fi "Olemassa oleva rakennus" :label-sv "Befintlig byggnad" :valid true}])

(defn find-laatimisvaiheet [] laatimisvaiheet)

(def ^:private vaihe-keys
  [:rakennuslupa,
   :kayttoonotto,
   :olemassaolevarakennus])

(defn vaihe-key [vaihe-id] (nth vaihe-keys vaihe-id))

(defn- in-vaihe? [vaihe-id energiatodistus]
  (= (-> energiatodistus :perustiedot :laatimisvaihe)
     vaihe-id))

(def rakennuslupa? (partial in-vaihe? 0))
(def kayttoonotto? (partial in-vaihe? 1))
(def olemassaoleva-rakennus? (partial in-vaihe? 2))
