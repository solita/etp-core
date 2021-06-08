(ns solita.etp.service.valvonta-kaytto)

(defonce state (atom {:valvonnat (sorted-map)
                      :henkilot (sorted-map)}))

(defn find! [k id]
  (let [found (some-> @state (get-in [k id]) (assoc :id id))]
    (when-not (:deleted? found)
      found)))

(defn add! [k coll]
  (-> (swap! state (fn [dereffed]
                     (let [id (-> dereffed (get k) keys last (or 0) inc)]
                       (assoc-in dereffed [k id] coll))))
      (get k)
      keys
      last))

(defn update! [k id coll]
  (let [new-state (swap! state
                         (fn [dereffed]
                           (if (and (contains? (get dereffed k) id)
                                    (-> dereffed
                                        (get-in [k id :deleted?])
                                        not))
                             (assoc-in dereffed [k id] coll)
                             dereffed)))]
    (if (contains? (get new-state k) id) 1 0)))

(defn delete! [k id]
  (let [[old new] (swap-vals! state
                              (fn [dereffed]
                                (if (contains? (get dereffed k) id)
                                  (assoc-in dereffed [k id :deleted?] true)
                                  dereffed)))]
    (if (= old new) 0 1)))

(defn find-valvonnat [_ {:keys [valvoja-id limit offset]}]
  (cond->> (:valvonnat @state)
    true (filter #(-> % second :deleted? not))
    valvoja-id (filter #(= valvoja-id (-> % second :valvoja-id)))
    offset (drop offset)
    limit (take limit)
    true (reduce (fn [acc [id valvonta]]
                   (conj acc (assoc valvonta :id id)))
                 [])))

(defn find-valvonta [_ valvonta-id]
  (find! :valvonnat valvonta-id))

(defn add-valvonta! [_ valvonta]
  (add! :valvonnat valvonta))

(defn update-valvonta! [_ valvonta-id valvonta]
  (update! :valvonnat valvonta-id valvonta))

(defn delete-valvonta! [_ valvonta-id]
  (delete! :valvonnat valvonta-id))

(defn find-ilmoituspaikat [_]
  (for [[idx label] (map-indexed vector ["Etuovi" "Oikotie" "Muu, mikä?"])]
    {:id idx
     :label-fi label
     :label-sv (str label " SV?")
     :valid true}))

(defn find-henkilot [_ valvonta-id]
  (->> (:henkilot @state)
       (filter #(-> % second :deleted? not))
       (filter #(= valvonta-id (-> % second :valvonta-id)))
       (reduce (fn [acc [id henkilo]]
                 (conj acc (assoc henkilo :id id)))
               [])))

(defn find-henkilo [_ henkilo-id]
  (find! :henkilot henkilo-id))

(defn add-henkilo! [_ valvonta-id henkilo]
  (add! :henkilot (assoc henkilo :valvonta-id valvonta-id)))

(defn update-henkilo! [_ henkilo-id henkilo]
  (update! :henkilot henkilo-id henkilo))

(defn delete-henkilo! [db henkilo-id]
  (delete! :henkilot henkilo-id))

(defn find-roolit [_]
  (for [[idx label] (map-indexed vector ["Omistaja"
                                         "Kiinteistövälittäjä"
                                         "Muu, mikä?"])]
    {:id idx
     :label-fi label
     :label-sv (str label " SV?")
     :valid true}))

(defn find-toimitustavat [_]
  (for [[idx label] (map-indexed vector ["Suomi.fi"
                                         "Sähköposti"
                                         "Muu, mikä?"])]
    {:id idx
     :label-fi label
     :label-sv (str label " SV?")
     :valid true}))
