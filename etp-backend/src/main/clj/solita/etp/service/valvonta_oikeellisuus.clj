(ns solita.etp.service.valvonta-oikeellisuus
  (:require
    [solita.etp.db :as db]
    [solita.etp.service.energiatodistus :as energiatodistus-service]
    [solita.etp.service.toimenpide :as toimenpide])
  (:import (java.time Instant)))

#_(db/require-queries 'valvonta-oikeellisuus)

(def valvonnat (atom {}))

(def default-valvonta
  {:active     false
   :liitteet   false
   :valvoja-id nil})

(defn find-valvonnat [db]
  (->> @valvonnat
       (map second)
       (map #(assoc % :last-toimenpide (-> % :toimenpiteet last)))
       (map #(dissoc % :toimenpiteet))
       (pmap #(assoc % :energiatodistus (energiatodistus-service/find-energiatodistus db (:id %))))))

(defn count-valvonnat [db] {:count (count @valvonnat)})

(defn find-valvonta [db id]
  (merge
    (assoc default-valvonta :id id)
    (-> @valvonnat (get id) (dissoc :toimenpiteet))))

(defn save-valvonta! [db id valvonta]
  (swap! valvonnat #(update % id (fn [current] (merge current (assoc valvonta :id id))))))

(defn- new-toimenpide [whoami id diaarinumero toimenpide toimenpiteet]
  (conj (or toimenpiteet [])
        (assoc toimenpide
          :id (count toimenpiteet)
          :energiatodistus-id id
          :author-id (:id whoami)
          :diaarinumero (or diaarinumero
                            (-> toimenpiteet last :diaarinumero))
          :create-time (Instant/now)
          :publish-time (when-not
                          (toimenpide/draft-support? toimenpide)
                          (Instant/now)))))

(defn default-to [default] #(or % default))

(defn insert-toimenpide! [db whoami id diaarinumero toimenpide]
  (-> valvonnat
      (swap! #(update-in % [id :toimenpiteet] (partial new-toimenpide whoami id diaarinumero toimenpide)))
      (get id) :toimenpiteet
      last))

;; asiahallinta integration
;; open case returns case number (diaarinumero)
(defn open-case! [db whoami id])
(defn log-toimenpide! [db whoami id toimenpide])
(defn close-case! [db whoami id toimenpide])

(defn add-toimenpide! [db whoami id toimenpide-add]
  (swap! valvonnat #(update % id (default-to (assoc default-valvonta :id id))))
  (let [diaarinumero (when (toimenpide/case-open? toimenpide-add)
                       (open-case! db whoami id))
        toimenpide (insert-toimenpide! db whoami id diaarinumero toimenpide-add)]
    (case (-> toimenpide :type-id toimenpide/type-key)
      :closed (close-case! db whoami id toimenpide)
      (when (and (toimenpide/published? toimenpide)
                 (toimenpide/asha-toimenpide? toimenpide))
        (log-toimenpide! db whoami id toimenpide)))
    (select-keys toimenpide [:id])))

(defn update-toimenpide [whoami id toimenpide toimenpiteet]
  (update toimenpiteet id #(merge % (assoc toimenpide :author-id (:id whoami)))))

(defn update-toimenpide! [db whoami id toimenpide-id toimenpide]
  (when (get-in @valvonnat [id :toimenpiteet toimenpide-id])
    (swap! valvonnat
           #(update-in % [id :toimenpiteet]
                       (partial update-toimenpide
                                whoami toimenpide-id toimenpide)))))

(defn find-toimenpiteet [db whoami id]
  (when-not
    (nil? (energiatodistus-service/find-energiatodistus db whoami id))
    (or (-> @valvonnat (get id) :toimenpiteet) [])))

(defn find-toimenpide [db whoami id toimenpide-id]
  (get (find-toimenpiteet db whoami id) toimenpide-id))

(defn publish-toimenpide! [db whoami id toimenpide-id]
  (log-toimenpide! db whoami id (find-toimenpide db whoami id toimenpide-id))
  (update-toimenpide! db whoami id toimenpide-id
                      { :publish-time (Instant/now) }))

(def toimenpidetyypit
  (map-indexed
    #(assoc %2 :id %1 :valid true)
    [{:label-fi "Katsottu" :label-sv "TODO"}
     {:label-fi "Poikkeama" :label-sv "TODO"}
     {:label-fi "Valvonnan aloitus" :label-sv "TODO"}
     {:label-fi "Tietopyyntö" :label-sv "TODO"}
     {:label-fi "Tietopyyntö / Vastaus" :label-sv "TODO"}
     {:label-fi "Tietopyyntö / Kehotus" :label-sv "TODO"}
     {:label-fi "Tietopyyntö / Varoitus" :label-sv "TODO"}
     {:label-fi "Valvontamuistio" :label-sv "TODO"}
     {:label-fi "Valvontamuistio / Vastaus" :label-sv "TODO"}
     {:label-fi "Valvontamuistio / Kehotus" :label-sv "TODO"}
     {:label-fi "Valvontamuistio / Varoitus" :label-sv "TODO"}
     {:label-fi "Kieltopäätös" :label-sv "TODO"}
     {:label-fi "Valvonnan lopetus" :label-sv "TODO"}]))

(defn find-toimenpidetyypit [db] toimenpidetyypit)

(defn- templates-for [toimenpidetype-id]
  [{:label-fi "Energiatodistus 2018" :label-sv "TODO" :language "fi" :toimenpidetype-id toimenpidetype-id}
   {:label-fi "Energiatodistus 2018" :label-sv "TODO" :language "sv" :toimenpidetype-id toimenpidetype-id}
   {:label-fi "Energiatodistus 2013" :label-sv "TODO" :language "fi" :toimenpidetype-id toimenpidetype-id}
   {:label-fi "Energiatodistus 2013" :label-sv "TODO" :language "sv" :toimenpidetype-id toimenpidetype-id}])

(defn find-templates [db]
  (map-indexed
    #(assoc %2 :id %1 :valid true)
    (flatten (map (comp templates-for :id) toimenpidetyypit))))
