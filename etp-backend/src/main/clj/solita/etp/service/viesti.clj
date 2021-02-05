(ns solita.etp.service.viesti
  (:require [clojure.set :as set])
  (:import (java.time Instant)))

(def ^:private ketjut (atom []))

(defn- sender [whoami]
  (-> whoami
      (select-keys  [:id :etunimi :sukunimi :rooli])
      (set/rename-keys {:rooli :rooli-id})))

(defn- viesti [whoami body]
  {:senttime (Instant/now)
   :from (sender whoami)
   :body body})

(defn add-ketju! [db whoami ketju]
  (-> ketjut
      (swap! #(conj %
        (-> ketju
            (assoc :id (count %))
            (assoc :viestit [(viesti whoami (:body ketju))])
            (dissoc :body))))
      count dec))

(defn find-ketju [db id] (get @ketjut id))

(defn find-ketjut [db] @ketjut)

(defn add-viesti! [db whoami id body]
  (when-not (nil? (find-ketju db id))
    (swap! ketjut #(update-in % [id :viestit] conj (viesti whoami body)))))

