(ns solita.etp.service.valvonta-oikeellisuus
  (:require
    [solita.etp.db :as db]
    [solita.etp.service.asha-valvonta-oikeellisuus :as asha-valvonta-oikeellisuus]
    [solita.etp.service.energiatodistus :as energiatodistus-service])
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

(defn- new-toimenpide [whoami id toimenpide toimenpiteet]
  (conj (or toimenpiteet [])
        (assoc toimenpide
          :id (count toimenpiteet)
          :energiatodistus-id id
          :author-id (:id whoami)
          :diaarinumero nil
          :create-time (Instant/now)
          :publish-time (Instant/now))))

(defn default-to [default] #(or % default))

(defn update-toimenpide [whoami id toimenpide toimenpiteet]
  (update toimenpiteet id #(merge % (assoc toimenpide :author-id (:id whoami)))))

(defn update-toimenpide! [db whoami id toimenpide-id toimenpide]
  (when (get-in @valvonnat [id :toimenpiteet toimenpide-id])
    (swap! valvonnat
           #(update-in % [id :toimenpiteet]
                       (partial update-toimenpide
                                whoami toimenpide-id toimenpide)))))

(defn asha-action! [db whoami id toimenpide]
  (case (:type-id toimenpide)
    2 (let [diaarinumero (asha-valvonta-oikeellisuus/create-case whoami db toimenpide)]
        (update-toimenpide! db whoami id (:id toimenpide) (assoc toimenpide :diaarinumero diaarinumero)))))

(defn add-toimenpide! [db whoami id toimenpide]
  (swap! valvonnat #(update % id (default-to (assoc default-valvonta :id id))))
  (let [toimenpide (-> valvonnat
                       (swap! #(update-in % [id :toimenpiteet] (partial new-toimenpide whoami id toimenpide)))
                       (get id) :toimenpiteet
                       last)]
    (asha-action! db whoami id toimenpide)
    (select-keys toimenpide [:id])))

(defn find-toimenpiteet [db whoami id]
  (when-not
    (nil? (energiatodistus-service/find-energiatodistus db whoami id))
    (or (-> @valvonnat (get id) :toimenpiteet) [])))

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
     {:label-fi "Valvonnan lopetus" :label-sv "TODO"}]))

(defn find-toimenpidetyypit [db] toimenpidetyypit)