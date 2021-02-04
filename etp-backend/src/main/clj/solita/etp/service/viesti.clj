(ns solita.etp.service.viesti
  (:import (java.time Instant)))

(def ^:private ketjut (atom []))

(defn- viesti [whoami body]
  {:senttime (Instant/now)
   :from-id (:id whoami)
   :body body})

(defn add-ketju! [db whoami ketju]
  (-> ketjut
      (swap! #(conj %
        (-> ketju
            (assoc :id (count %))
            (assoc :from-id (:id whoami))
            (assoc :viestit [(viesti whoami (:body ketju))])
            (dissoc :body))))
      count dec))

(defn find-ketju [db id] (get @ketjut id))

(defn find-ketjut [db] @ketjut)

(defn add-viesti! [db whoami id body]
  (when-not (nil? (find-ketju db id))
    (swap! ketjut #(update-in % [id :viestit] conj (viesti whoami body)))))

