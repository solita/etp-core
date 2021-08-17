(ns solita.etp.service.valvonta-kaytto
  (:require [solita.etp.service.valvonta-kaytto.asha :as asha]
            [solita.etp.service.valvonta-kaytto.toimenpide :as toimenpide]
            [clojure.java.io :as io]
            [solita.etp.service.pdf :as pdf]
            [clojure.set :as set]
            [solita.common.logic :as logic]
            [solita.etp.service.file :as file-service]
            [solita.etp.db :as db]
            [clojure.java.jdbc :as jdbc]
            [solita.etp.service.luokittelu :as luokittelu])
  (:import (java.time Instant)))

(db/require-queries 'valvonta-kaytto)

(defonce state (atom {:valvonnat (sorted-map)
                      :henkilot (sorted-map)
                      :yritykset (sorted-map)
                      :liitteet (sorted-map)}))

(def valvonta-toimenpiteet (atom {}))

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

(defn valvonta-related [id coll]
  (->> coll
       (filter #(= id (-> % second :valvonta-id)))
       (filter #(-> % second :deleted? not))))
#_
(defn find-test-valvonnat [db {:keys [valvoja-id limit offset]}]
  (let [valvonnat (valvonta-kaytto-db/select)]))


(defn find-henkilot [db valvonta-id]
  (valvonta-kaytto-db/select-henkilot db {:valvonta-id valvonta-id}))

(defn find-yritykset [db valvonta-id]
  (valvonta-kaytto-db/select-yritykset db {:valvonta-id valvonta-id}))

(defn find-valvonnat [db whoami query]
  (let [valvonnat (valvonta-kaytto-db/select-valvonnat db
                                                            (merge {:limit 10 :offset 0 :valvoja-id (:id whoami)} query))]
    (->> valvonnat
         (map (fn [valvonta]
                (-> valvonta
                    (assoc :henkilot (->> (find-henkilot db (:id valvonta))
                                          (map #(select-keys % [:id :rooli-id :etunimi :sukunimi])))
                           :yritykset  (->> (find-yritykset db (:id valvonta))
                                            (map #(select-keys % [:id :rooli-id :nimi])))
                           :last-toimenpide nil)))))))

(defn find-valvonnat2 [_ {:keys [valvoja-id limit offset]}]
  (let [{:keys [valvonnat henkilot yritykset]} @state]
    (cond->> valvonnat
      true (filter #(-> % second :deleted? not))
      valvoja-id (filter #(= valvoja-id
                             (-> % second :valvoja-id)))
      offset (drop offset)
      limit (take limit)
      true (reduce (fn [acc [id valvonta]]
                     (conj acc (assoc valvonta :id id)))
                   [])
      true (map (fn [{:keys [id] :as valvonta}]
                  (assoc valvonta
                         :henkilot
                         (->> henkilot
                              (valvonta-related id)
                              (map (fn [[id henkilo]]
                                     (-> henkilo
                                         (select-keys [:etunimi
                                                       :sukunimi
                                                       :rooli-id])
                                         (assoc :id id))))))))
      true (map (fn [{:keys [id] :as valvonta}]
                  (assoc valvonta
                         :yritykset
                         (->> yritykset
                              (valvonta-related id)
                              (map (fn [[id yritys]]
                                     (-> yritys
                                         (select-keys [:nimi :rooli-id])
                                         (assoc :id id))))))))
      true (map (fn [{:keys [id] :as valvonta}]
                  (assoc valvonta
                         :last-toimenpide
                         (some-> @valvonta-toimenpiteet
                                 (get id)
                                 last
                                 (select-keys [:type-id :deadline-date]))))))))

(defn count-valvonnat [db whoami query]
  (first (valvonta-kaytto-db/select-valvonnat-count db
                                                      (merge {:limit 10 :offset 0 :valvoja-id (:id whoami)} query))))

(defn find-valvonta [db valvonta-id]
  (first (valvonta-kaytto-db/select-valvonta db {:id valvonta-id})))

(defn add-valvonta! [db valvonta]
  (-> (db/with-db-exception-translation
           jdbc/insert! db :vk-valvonta
           (dissoc valvonta :valvoja-id)
           db/default-opts) first :id))

(defn update-valvonta! [db valvonta-id valvonta]
  (first (db/with-db-exception-translation
           jdbc/update! db :vk_valvonta
           valvonta ["id = ?" valvonta-id]
           db/default-opts)))

(defn delete-valvonta! [db valvonta-id]
  (let [valvonta (find-valvonta db valvonta-id)]
    (first (db/with-db-exception-translation
             jdbc/update! db :vk_valvonta
             (assoc valvonta :deleted true)
             ["id = ?" valvonta-id]
             db/default-opts))))

(defn find-ilmoituspaikat [db]
  (luokittelu/find-vk-ilmoituspaikat db))



(defn find-henkilo [db henkilo-id]
  (first (valvonta-kaytto-db/select-henkilo db {:id henkilo-id})))

(defn add-henkilo! [db valvonta-id henkilo]
  (-> (db/with-db-exception-translation
        jdbc/insert! db :vk_henkilo
        (assoc henkilo :valvonta-id valvonta-id)
        db/default-opts) first :id))

(defn update-henkilo! [db valvonta-id henkilo-id henkilo]
  (first (db/with-db-exception-translation
        jdbc/update! db :vk_henkilo
        (assoc henkilo :valvonta-id valvonta-id)
        ["id = ?" henkilo-id]
        db/default-opts)))

(defn delete-henkilo! [db henkilo-id]
  (let [henkilo (find-henkilo db henkilo-id)]
    (first (db/with-db-exception-translation
             jdbc/update! db :vk_henkilo
             (assoc henkilo :deleted true)
             ["id = ?" henkilo-id]
             db/default-opts))))

(defn find-roolit [db]
  (luokittelu/find-vk-roolit db))

(defn find-toimitustavat [db]
  (luokittelu/find-vk-toimitustavat db))

(defn find-yritys [db yritys-id]
  (first (valvonta-kaytto-db/select-yritys db {:id yritys-id})))

(defn add-yritys! [db valvonta-id yritys]
  (-> (db/with-db-exception-translation
        jdbc/insert! db :vk_yritys
        (assoc yritys :valvonta-id valvonta-id)
        db/default-opts) first :id))

(defn update-yritys! [db valvonta-id yritys-id yritys]
  (first (db/with-db-exception-translation
           jdbc/update! db :vk_yritys
           (assoc yritys :valvonta-id valvonta-id)
           ["id = ?" yritys-id]
           db/default-opts)))

(defn delete-yritys! [db yritys-id]
  (let [yritys (find-yritys db yritys-id)]
    (first (db/with-db-exception-translation
             jdbc/update! db :vk_yritys
             (assoc yritys :deleted true)
             ["id = ?" yritys-id]
             db/default-opts))))

(defn find-liitteet [_ valvonta-id]
  (->> (:liitteet @state)
       (filter #(-> % second :deleted? not))
       (filter #(= valvonta-id (-> % second :valvonta-id)))
       (reduce (fn [acc [id liite]]
                 (conj acc (-> liite
                               (dissoc :valvonta-id :content-type :filename :tempfile :size)
                               (assoc :id id
                                      :nimi (or (:filename liite) (:nimi liite))
                                      :url (:url liite)
                                      :createtime (java.time.Instant/now)
                                      :author-fullname "Liisa Specimen-Potex"
                                      :contenttype "application/pdf"))))
               [])))


(defn file-path [valvonta-id liite-id]
  (str "/valvonta/kaytto/" valvonta-id "/liitteet/" liite-id))

(defn add-liitteet-from-files! [aws-s3-client valvonta-id liitteet]
  (doseq [liite liitteet]
    (let [liite-id (add! :liitteet (assoc liite :valvonta-id valvonta-id))]
      (file-service/upsert-file-from-file
        aws-s3-client
        (file-path valvonta-id liite-id)
        (:tempfile liite)))))

(defn add-liite-from-link! [db valvonta-id liite]
  (add! :liitteet (assoc liite :valvonta-id valvonta-id)))

(defn delete-liite! [_ _ liite-id]
  (delete! :liitteet liite-id))

(defn find-liite [aws-s3-client valvonta-id liite-id]
  (when-let [liite (find! :liitteet liite-id)]
    (assoc liite :content
                 (file-service/find-file aws-s3-client (file-path valvonta-id liite-id)))))

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

(defn find-toimenpiteet [db valvonta-id]
  (when-not (nil? (find-valvonta db valvonta-id))
    (or (@valvonta-toimenpiteet valvonta-id) [])))

(defn find-toimenpide [db valvonta-id toimenpide-id]
  (get-in @valvonta-toimenpiteet [valvonta-id toimenpide-id]))

(defn toimenpide-filename [toimenpide]
  (:filename (asha/toimenpide-type->document (:type-id toimenpide))))

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
                                :filename (toimenpide-filename toimenpide-add)
                                :diaarinumero diaarinumero)))))
      (get valvonta-id)
      last))

(defn- find-diaarinumero [db valvonta-id toimenpide]
  (-> (find-toimenpiteet db valvonta-id)
      last
      :diaarinumero))

(defn add-toimenpide! [db aws-s3-client whoami valvonta-id toimenpide-add]
  (let [osapuolet (concat
                    (find-henkilot db valvonta-id)
                    (find-yritykset db valvonta-id))
        diaarinumero (if (toimenpide/case-open? toimenpide-add)
                       (asha/open-case!
                         db
                         whoami
                         (find-valvonta db valvonta-id)
                         osapuolet
                         (find-ilmoituspaikat db))
                       (find-diaarinumero db valvonta-id toimenpide-add))
        toimenpide (insert-toimenpide! db whoami valvonta-id diaarinumero toimenpide-add)
        toimenpide-id (:id toimenpide)]
    (case (-> toimenpide :type-id toimenpide/type-key)
      :closed (asha/close-case! whoami valvonta-id toimenpide)
      (when (toimenpide/asha-toimenpide? toimenpide)
        (asha/log-toimenpide!
          db
          aws-s3-client
          whoami
          (find-valvonta db valvonta-id)
          toimenpide
          osapuolet
          (find-ilmoituspaikat db))))
    {:id toimenpide-id}))

(defn update-toimenpide! [db valvonta-id toimenpide-id toimenpide-update]
  (swap! valvonta-toimenpiteet
         #(update-in % [valvonta-id toimenpide-id]
                     (fn [toimenpide] (merge toimenpide toimenpide-update)))))

(defn- preview-toimenpide [db whoami id toimenpide maybe-osapuoli]
  (logic/if-let*
    [osapuoli maybe-osapuoli
     valvonta (find-valvonta db id)
     {:keys [template template-data]} (asha/generate-template
                                        db
                                        whoami
                                        (find-valvonta db id)
                                        toimenpide
                                        osapuoli
                                        (find-ilmoituspaikat db))]
    (pdf/template->pdf-input-stream template template-data)))

(defn preview-toimenpide [db whoami id toimenpide osapuoli]
  (when-let [{:keys [template template-data]} (asha/generate-template
                                                db
                                                whoami
                                                (find-valvonta db id)
                                                toimenpide
                                                osapuoli
                                                (find-ilmoituspaikat db))]
    (pdf/template->pdf-input-stream template template-data)))


(defn preview-henkilo-toimenpide [db whoami id toimenpide henkilo-id]
  (preview-toimenpide db whoami id toimenpide (find-henkilo db henkilo-id)))

(defn preview-yritys-toimenpide [db whoami id toimenpide yritys-id]
  (preview-toimenpide db whoami id toimenpide (find-yritys db yritys-id)))

(defn find-toimenpide-henkilo-document [db aws-s3-client valvonta-id toimenpide-id henkilo-id]
  (asha/find-document aws-s3-client valvonta-id toimenpide-id (find-henkilo db henkilo-id)))

(defn find-toimenpide-yritys-document [db aws-s3-client valvonta-id toimenpide-id yritys-id]
  (asha/find-document aws-s3-client valvonta-id toimenpide-id (find-yritys db yritys-id)))

(defn find-toimenpide-document [aws-s3-client valvonta-id toimenpide-id osapuoli ostream]
  (when-let [document (asha/find-document aws-s3-client valvonta-id toimenpide-id osapuoli)]
    (with-open [output (io/output-stream ostream)]
      (io/copy document output))))
