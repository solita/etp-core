(ns solita.etp.service.valvonta-kaytto)

(defonce state (atom (sorted-map)))

(defn find-valvonnat [_ {:keys [valvoja-id limit offset]}]
  (cond->> @state
    valvoja-id (filter #(= valvoja-id (-> % second :valvoja-id)))
    offset (drop offset)
    limit (take limit)
    true (reduce (fn [acc [id valvonta]]
                   (conj acc (assoc valvonta :id id)))
                 [])))

(defn find-valvonta [_ id]
  (some-> @state (get id) (assoc :id id)))

(defn add-valvonta! [_ valvonta]
  (-> (swap! state (fn [valvonnat]
                     (assoc valvonnat
                            (-> valvonnat keys last (or 0) inc)
                            valvonta)))
      keys
      last))

(defn update-valvonta! [_ id body]
  (let [new-valvonnat (swap! state (fn [valvonnat]
                                     (if (contains? valvonnat id)
                                       (assoc valvonnat id body)
                                       valvonnat)))]
    (if (contains? new-valvonnat id)
      1
      0)))

(defn delete-valvonta! [_ id]
  (swap! state dissoc id))
