(ns solita.etp.service.valvonta-oikeellisuus
  (:require [solita.etp.service.energiatodistus :as energiatodistus-service])
  (:import (java.time Instant)))

(def toimenpiteet (atom {}))
(def valvonta (atom {}))

(defn find-valvonnat [db]
  (->> @toimenpiteet
       (map (comp last second))
       (pmap #(assoc % :energiatodistus (energiatodistus-service/find-energiatodistus db (:energiatodistus-id %))))))

(defn- new-toimenpide [whoami id toimenpide toimenpiteet]
  (conj (or toimenpiteet [])
        (assoc toimenpide
          :id (count toimenpiteet)
          :energiatodistus-id id
          :author-id (:id whoami)
          :diaarinumero nil
          :create-time (Instant/now)
          :publish-time (Instant/now))))

(defn add-toimenpide! [db whoami id toimenpide]
  (-> toimenpiteet
      (swap! #(update % id (partial new-toimenpide whoami id toimenpide)))
      (get id)
      last
      (select-keys [:id])))

(defn update-toimenpide [whoami id toimenpide toimenpiteet]
  (update toimenpiteet id #(merge % (assoc toimenpide :author-id (:id whoami)))))

(defn update-toimenpide! [db whoami id toimenpide-id toimenpide]
  (when (get-in @toimenpiteet [id toimenpide-id])
    (swap! toimenpiteet
           #(update % id (partial update-toimenpide
                                  whoami toimenpide-id toimenpide)))))

(defn find-toimenpiteet [db whoami id]
  (when-not
    (nil? (energiatodistus-service/find-energiatodistus db whoami id))
    (or (@toimenpiteet id) [])))

(def toimenpidetyypit
  (map-indexed
    #(assoc %2 :id %1 :valid true)
    [{:label-fi "Poikkeama" :label-sv "TODO"}
     {:label-fi "Katsottu" :label-sv "TODO"}
     {:label-fi "Valvonnan vireilletulo" :label-sv "TODO"}
     {:label-fi "Tietopyyntö / Lähetetty" :label-sv "TODO"}
     {:label-fi "Tietopyyntö / Vastaus" :label-sv "TODO"}
     {:label-fi "Tietopyyntö / Kehotus" :label-sv "TODO"}
     {:label-fi "Tietopyyntö / Varoitus" :label-sv "TODO"}
     {:label-fi "Valvontamuistio / Lähetetty" :label-sv "TODO"}
     {:label-fi "Valvontamuistio / Vastaus" :label-sv "TODO"}
     {:label-fi "Valvontamuistio / Kehotus" :label-sv "TODO"}
     {:label-fi "Valvontamuistio / Varoitus" :label-sv "TODO"}
     {:label-fi "Valvonta päätetty" :label-sv "TODO"}]))

(defn find-toimenpidetyypit [db] toimenpidetyypit)