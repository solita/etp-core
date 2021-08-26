(ns solita.etp.service.valvonta-kaytto
  (:require [solita.etp.service.valvonta-kaytto.asha :as asha]
            [solita.etp.service.valvonta-kaytto.toimenpide :as toimenpide]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [solita.etp.service.file :as file-service]
            [solita.etp.db :as db]
            [clojure.java.jdbc :as jdbc]
            [solita.etp.service.luokittelu :as luokittelu]
            [flathead.flatten :as flat]
            [solita.etp.schema.valvonta-kaytto :as kaytto-schema]
            [solita.common.maybe :as maybe])
  (:import (java.time Instant)))

(db/require-queries 'valvonta-kaytto)

(defn find-henkilot [db valvonta-id]
  (valvonta-kaytto-db/select-henkilot db {:valvonta-id valvonta-id}))

(defn find-yritykset [db valvonta-id]
  (valvonta-kaytto-db/select-yritykset db {:valvonta-id valvonta-id}))

(defn find-valvonta-henkilot [db valvonta-id]
  (->> (find-henkilot db valvonta-id)
       (map #(select-keys % [:id :rooli-id :etunimi :sukunimi]))))

(defn find-valvonta-yritykset [db valvonta-id]
  (->> (find-yritykset db valvonta-id)
       (mapv #(select-keys % [:id :rooli-id :nimi]))))

(defn- find-valvonta-last-toimenpide [db valvonta-id]
  (first
    (valvonta-kaytto-db/select-last-toimenpide
      db
      {:valvonta-id valvonta-id})))

(defn find-valvonnat [db whoami query]
  (->> (valvonta-kaytto-db/select-valvonnat db
                                            (merge {:limit 10 :offset 0 :valvoja-id (:id whoami)} query))
       (map (fn [valvonta]
              (-> valvonta
                  (assoc :henkilot (find-valvonta-henkilot db (:id valvonta))
                         :yritykset (find-valvonta-yritykset db (:id valvonta))
                         :last-toimenpide (find-valvonta-last-toimenpide db (:id valvonta))))))))

(defn count-valvonnat [db whoami query]
  (first
    (valvonta-kaytto-db/select-valvonnat-count db
                                               (merge
                                                 {:limit 10 :offset 0 :valvoja-id (:id whoami)}
                                                 query))))

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
  (valvonta-kaytto-db/delete-valvonta! db {:id valvonta-id}))

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
  (valvonta-kaytto-db/delete-henkilo! db {:id henkilo-id}))

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
  (valvonta-kaytto-db/delete-yritys! db {:id yritys-id}))

(defn find-liitteet [db valvonta-id]
  (valvonta-kaytto-db/select-liite-by-valvonta-id db {:valvonta-id valvonta-id}))

(defn file-path [valvonta-id liite-id]
  (str "/valvonta/kaytto/" valvonta-id "/liitteet/" liite-id))

(defn- insert-liite! [db liite]
  (-> (db/with-db-exception-translation jdbc/insert! db :vk_valvonta_liite liite db/default-opts)
      first
      :id))

(defn add-liitteet-from-files! [db aws-s3-client valvonta-id liitteet]
  (doseq [liite liitteet]
    (let [liite-id (insert-liite! db (-> liite
                                         (dissoc :tempfile :size)
                                         (assoc :valvonta-id valvonta-id)
                                         (set/rename-keys {:content-type :contenttype
                                                           :filename     :nimi})))]
      (file-service/upsert-file-from-file
        aws-s3-client
        (file-path valvonta-id liite-id)
        (:tempfile liite)))))

(defn add-liite-from-link! [db valvonta-id liite]
  (insert-liite! db (assoc liite :contenttype "text/uri-list"
                                 :valvonta-id valvonta-id)))

(defn delete-liite! [db liite-id]
  (valvonta-kaytto-db/delete-liite! db {:id liite-id}))

(defn find-liite [db aws-s3-client valvonta-id liite-id]
  (when-let [liite (first (valvonta-kaytto-db/select-liite db {:id liite-id}))]
    (assoc liite :tempfile
                 (file-service/find-file aws-s3-client (file-path valvonta-id liite-id)))))

(defn find-toimenpidetyypit [db]
  (luokittelu/find-vk-toimenpidetypes db))

(defn find-templates [db]
  (valvonta-kaytto-db/select-templates db))

(defn toimenpide-filename [toimenpide]
  (:filename (asha/toimenpide-type->document (:type-id toimenpide))))

(defn- db-row->toimenpide [db-row]
  (as-> db-row %
        (flat/flat->tree #"\$" %)
        (assoc % :filename (toimenpide-filename %))))

(defn- find-toimenpide-henkilot [db toimenpide-id]
  (->> (valvonta-kaytto-db/select-toimenpide-henkilo db {:toimenpide-id toimenpide-id})
       (map :henkilo-id)))

(defn- find-toimenpide-yritykset [db toimenpide-id]
  (->> (valvonta-kaytto-db/select-toimenpide-yritys db {:toimenpide-id toimenpide-id})
       (map :yritys-id)))

(defn find-toimenpiteet [db valvonta-id]
  (->> (valvonta-kaytto-db/select-toimenpiteet db {:valvonta-id valvonta-id})
       (map db-row->toimenpide)
       (map #(assoc % :henkilot (find-toimenpide-henkilot db (:id %))
                      :yritykset (find-toimenpide-yritykset db (:id %))))))

(defn find-toimenpide [db toimenpide-id]
  (valvonta-kaytto-db/select-toimenpide db {:id toimenpide-id}))

(defn- insert-toimenpide-henkilo! [db toimenpide-id henkilo-id]
  (db/with-db-exception-translation
    jdbc/insert! db :vk_toimenpide_henkilo
    {:toimenpide-id toimenpide-id
     :henkilo-id    henkilo-id}
    db/default-opts))

(defn- insert-toimenpide-yritys! [db toimenpide-id yritys-id]
  (db/with-db-exception-translation
    jdbc/insert! db :vk_toimenpide_yritys
    {:toimenpide-id toimenpide-id
     :yritys-id     yritys-id}
    db/default-opts))

(defn- insert-toimenpide-osapuolet! [db toimenpide-id osapuolet]
  (doseq [osapuoli osapuolet]
    (cond
      (kaytto-schema/henkilo? osapuoli) (insert-toimenpide-henkilo! db toimenpide-id (:id osapuoli))
      (kaytto-schema/yritys? osapuoli) (insert-toimenpide-yritys! db toimenpide-id (:id osapuoli)))))

(defn- insert-toimenpide! [db valvonta-id diaarinumero toimenpide-add]
  (first (db/with-db-exception-translation
           jdbc/insert! db :vk-toimenpide
           (assoc toimenpide-add
             :diaarinumero diaarinumero
             :valvonta_id valvonta-id
             :publish-time (Instant/now))
           db/default-opts)))

(defn find-diaarinumero [db id toimenpide]
  (when (or (toimenpide/asha-toimenpide? toimenpide)
            (toimenpide/case-closed? toimenpide))
    (-> (valvonta-kaytto-db/select-last-diaarinumero db {:id id})
        first :diaarinumero)))

(defn add-toimenpide! [db aws-s3-client whoami valvonta-id toimenpide-add]
  (jdbc/with-db-transaction [db db]
                            (let [osapuolet (concat
                                              (find-henkilot db valvonta-id)
                                              (find-yritykset db valvonta-id))
                                  valvonta    (find-valvonta db valvonta-id)
                                  ilmoituspaikat (find-ilmoituspaikat db)
                                  diaarinumero (if (toimenpide/case-open? toimenpide-add)
                                                 (asha/open-case!
                                                   db
                                                   whoami
                                                   valvonta
                                                   osapuolet
                                                   ilmoituspaikat)
                                                 (find-diaarinumero db valvonta-id toimenpide-add))
                                  toimenpide (insert-toimenpide! db valvonta-id diaarinumero toimenpide-add)
                                  toimenpide-id (:id toimenpide)]
                              (insert-toimenpide-osapuolet! db toimenpide-id osapuolet)
                              (case (-> toimenpide :type-id toimenpide/type-key)
                                :closed (asha/close-case! whoami valvonta-id toimenpide)
                                (when (toimenpide/asha-toimenpide? toimenpide)
                                  (asha/log-toimenpide!
                                    db
                                    aws-s3-client
                                    whoami
                                    valvonta
                                    toimenpide
                                    osapuolet
                                    ilmoituspaikat)))
                              {:id toimenpide-id})))

(defn update-toimenpide! [db toimenpide-id toimenpide]
  (first (db/with-db-exception-translation
           jdbc/update! db :vk_toimenpide
           toimenpide ["id = ?" toimenpide-id]
           db/default-opts)))

(defn preview-toimenpide [db whoami id toimenpide osapuoli]
  (maybe/map*
    io/input-stream
    (asha/generate-pdf-document
      db
      whoami
      (find-valvonta db id)
      (assoc toimenpide :diaarinumero (find-diaarinumero db id toimenpide))
      (find-ilmoituspaikat db)
      osapuoli)))


(defn preview-henkilo-toimenpide [db whoami id toimenpide henkilo-id]
  (preview-toimenpide
    db
    whoami
    id
    toimenpide
    (find-henkilo db henkilo-id)))

(defn preview-yritys-toimenpide [db whoami id toimenpide yritys-id]
  (preview-toimenpide
    db
    whoami
    id
    toimenpide
    (find-yritys db yritys-id)))

(defn find-toimenpide-henkilo-document [db aws-s3-client valvonta-id toimenpide-id henkilo-id]
  (asha/find-document aws-s3-client valvonta-id toimenpide-id (find-henkilo db henkilo-id)))

(defn find-toimenpide-yritys-document [db aws-s3-client valvonta-id toimenpide-id yritys-id]
  (asha/find-document aws-s3-client valvonta-id toimenpide-id (find-yritys db yritys-id)))

(defn find-toimenpide-document [aws-s3-client valvonta-id toimenpide-id osapuoli ostream]
  (when-let [document (asha/find-document aws-s3-client valvonta-id toimenpide-id osapuoli)]
    (with-open [output (io/output-stream ostream)]
      (io/copy document output))))