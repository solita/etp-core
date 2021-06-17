(ns solita.etp.service.valvonta-kaytto
  (:require [solita.etp.service.valvonta-kaytto.asha :as asha]
            [solita.etp.service.valvonta-kaytto.toimenpide :as toimenpide]
            [clojure.java.io :as io]
            [solita.etp.service.pdf :as pdf]
            [clojure.set :as set])
  (:import (java.time Instant)))

(defonce state (atom {:valvonnat (sorted-map)
                      :henkilot (sorted-map)
                      :yritykset (sorted-map)
                      :liitteet (sorted-map)}))

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

(defn find-yritykset [_ valvonta-id]
  (->> (:yritykset @state)
       (filter #(-> % second :deleted? not))
       (filter #(= valvonta-id (-> % second :valvonta-id)))
       (reduce (fn [acc [id yritys]]
                 (conj acc (assoc yritys :id id)))
               [])))

(defn find-yritys [_ yritys-id]
  (find! :yritykset yritys-id))

(defn add-yritys! [_ valvonta-id yritys]
  (add! :yritykset (assoc yritys :valvonta-id valvonta-id)))

(defn update-yritys! [_ yritys-id yritys]
  (update! :yritykset yritys-id yritys))

(defn delete-yritys! [_ yritys-id]
  (delete! :yritykset yritys-id))

(defn find-liitteet [_ valvonta-id]
  (->> (:liitteet @state)
       (filter #(-> % second :deleted? not))
       (filter #(= valvonta-id (-> % second :valvonta-id)))
       (reduce (fn [acc [id liite]]
                 (conj acc (-> liite
                               (dissoc :valvonta-id)
                               (assoc :id id
                                      :createtime (java.time.Instant/now)
                                      :author-fullname "Liisa Specimen-Potex"
                                      :contenttype "application/pdf"))))
               [])))

(defn add-liitteet-from-files! [_ valvonta-id liite]
  (add! :liitteet (assoc liite :valvonta-id valvonta-id)))

(defn add-liite-from-link! [db valvonta-id liite]
  (add! :liitteet (assoc liite :valvonta-id valvonta-id)))

(defn delete-liite! [_ liite-id]
  (delete! :liitteet liite-id))

(defn find-toimenpidetyypit [db]
  (for [[idx label] (map-indexed vector ["Valvonnan aloitus"
                                         "Tietopyyntö" "Tietopyyntö / Kehotus" "Tietopyyntö / Varoitus"
                                         "Käskypäätös"
                                         "Valvonnan lopetus"])]
    {:id idx
     :label-fi label
     :label-sv (str label " SV?")
     :valid true}))

(defn find-templates [db]
  (for [[idx label] (map-indexed vector ["Tietopyyntö" "Tietopyyntö / Kehotus" "Tietopyyntö / Varoitus"])]
    {:id idx
     :label-fi label
     :label-sv (str label " SV?")
     :toimenpidetype-id (inc idx)
     :language "fi"
     :valid true}))

(def valvonta-toimenpiteet (atom {}))

(defn find-toimenpiteet [db valvonta-id]
  (when-not (nil? (find-valvonta db valvonta-id))
    (or (@valvonta-toimenpiteet valvonta-id) [])))

(defn find-toimenpide [db valvonta-id toimenpide-id]
  (get-in @valvonta-toimenpiteet [valvonta-id toimenpide-id]))

(defn- insert-toimenpide! [db whoami valvonta-id diaarinumero toimenpide-add]
  (-> valvonta-toimenpiteet
      (swap! #(update % valvonta-id
                      (fn [toimenpiteet]
                        (conj (or toimenpiteet [])
                              (assoc toimenpide-add
                                :id (count toimenpiteet)
                                :publish-time (Instant/now)
                                :create-time (Instant/now)
                                :author (-> whoami
                                            (select-keys [:id :etunimi :sukunimi :rooli])
                                            (set/rename-keys {:rooli :rooli-id}))
                                :filename "test.pdf"
                                :diaarinumero diaarinumero)))))
      (get valvonta-id)
      last))

(defn- find-diaarinumero [db valvonta-id toimenpide]
  (-> (find-toimenpiteet db valvonta-id)
      last
      :diaarinumero))

(defn add-toimenpide! [db aws-s3-client whoami valvonta-id toimenpide-add]
  (let [diaarinumero (if (toimenpide/case-open? toimenpide-add)
                       (asha/open-case!
                         db
                         whoami
                         (find-valvonta db valvonta-id)
                         (find-henkilot db valvonta-id)
                         (find-ilmoituspaikat db))
                       (find-diaarinumero db valvonta-id toimenpide-add))
        toimenpide (insert-toimenpide! db whoami valvonta-id diaarinumero toimenpide-add)
        toimenpide-id (:id toimenpide)]
    (case (-> toimenpide :type-id toimenpide/type-key)
      :closed (asha/close-case! whoami valvonta-id toimenpide)
      (when (toimenpide/asha-toimenpide? toimenpide)
        (let [valvonta (find-valvonta db valvonta-id)
              henkilot (find-henkilot db valvonta-id)
              ilmoituspaikat (find-ilmoituspaikat db)]
          (asha/log-toimenpide! db aws-s3-client whoami valvonta toimenpide henkilot ilmoituspaikat))))
    {:id toimenpide-id}))

(defn update-toimenpide! [db valvonta-id toimenpide-id toimenpide-update]
  (swap! valvonta-toimenpiteet
         #(update-in % [valvonta-id toimenpide-id]
                     (fn [toimenpide] (merge toimenpide toimenpide-update)))))

(defn toimenpide-filename [toimenpide] "test.pdf")

(defn preview-toimenpide [db whoami id toimenpide ostream]
  (let [valvonta (find-valvonta db id)
        ilmoituspaikat (find-ilmoituspaikat db)]
    (when-let [{:keys [template template-data]} (asha/generate-template db whoami id valvonta toimenpide ilmoituspaikat)]
      (with-open [output (io/output-stream ostream)]
        (pdf/html->pdf template template-data output)))))

(defn find-toimenpide-document [aws-s3-client valvonta-id toimenpide-id ostream]
  (when-let [document (asha/find-document aws-s3-client valvonta-id toimenpide-id)]
    (with-open [output (io/output-stream ostream)]
      (io/copy document output))))