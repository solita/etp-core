(ns solita.etp.service.laatimisvaihe
  (:require [solita.etp.service.luokittelu :as luokittelu-service]))

(def find-laatimisvaiheet luokittelu-service/find-laatimisvaiheet)

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
