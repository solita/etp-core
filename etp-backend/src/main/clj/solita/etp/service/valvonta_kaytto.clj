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
            [solita.common.maybe :as maybe]
            [solita.common.map :as map]
            [solita.common.logic :as logic]
            [solita.etp.service.valvonta-kaytto.email :as email]
            [solita.etp.exception :as exception]
            [solita.etp.service.concurrent :as concurrent])
  (:import (java.time Instant)))

(db/require-queries 'valvonta-kaytto)

(defn find-henkilot [db valvonta-id]
  (valvonta-kaytto-db/select-henkilot db {:valvonta-id valvonta-id}))

(defn find-yritykset [db valvonta-id]
  (valvonta-kaytto-db/select-yritykset db {:valvonta-id valvonta-id}))

(defn- db-row->valvonta [valvonta-db-row]
  (update valvonta-db-row :postinumero (maybe/lift1 #(format "%05d" %))))

(def ^:private default-valvonta-query
  {:valvoja-id nil :has-valvoja nil :include-closed false
   :limit 10 :offset 0})

(defn- nil-if-not-exists [key object]
  (update object key (logic/when* (comp nil? :id) (constantly nil))))

(defn find-valvonnat [db query]
  (->> (valvonta-kaytto-db/select-valvonnat db (merge default-valvonta-query query))
       (map (comp (partial nil-if-not-exists :last-toimenpide)
                  (partial nil-if-not-exists :energiatodistus)
                  db-row->valvonta
                  #(flat/flat->tree #"\$" %)))
       (pmap #(assoc % :henkilot (find-henkilot db (:id %))
                       :yritykset (find-yritykset db (:id %))))))

(defn count-valvonnat [db query]
  (first
    (valvonta-kaytto-db/select-valvonnat-count
      db (merge default-valvonta-query query))))

(defn find-valvonta [db valvonta-id]
  (->> (valvonta-kaytto-db/select-valvonta db {:id valvonta-id})
       (map db-row->valvonta)
       first))

(defn- valvonta->db-row [valvonta]
  (update valvonta :postinumero (maybe/lift1 #(Integer/parseInt %))))

(defn add-valvonta! [db valvonta]
  (-> (db/with-db-exception-translation
        jdbc/insert! db :vk-valvonta
        (valvonta->db-row valvonta)
        db/default-opts)
      first
      :id))

(defn update-valvonta! [db valvonta-id valvonta]
  (first (db/with-db-exception-translation
           jdbc/update! db :vk_valvonta
           (valvonta->db-row valvonta)
           ["id = ?" valvonta-id]
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
        db/default-opts)
      first
      :id))

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
        db/default-opts)
      first
      :id))

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
  (valvonta-kaytto-db/select-toimenpide-henkilo db {:toimenpide-id toimenpide-id}))

(defn- find-toimenpide-yritykset [db toimenpide-id]
  (valvonta-kaytto-db/select-toimenpide-yritys db {:toimenpide-id toimenpide-id}))

(defn find-toimenpiteet [db valvonta-id]
  (->> (valvonta-kaytto-db/select-toimenpiteet db {:valvonta-id valvonta-id})
       (map db-row->toimenpide)
       (pmap #(assoc % :henkilot (find-toimenpide-henkilot db (:id %))
                       :yritykset (find-toimenpide-yritykset db (:id %))))))

(defn- insert-toimenpide-osapuolet! [db valvonta-id toimenpide-id]
  (let [params (map/bindings->map valvonta-id toimenpide-id)]
    (valvonta-kaytto-db/insert-toimenpide-henkilot! db params)
    (valvonta-kaytto-db/insert-toimenpide-yritykset! db params)))

(defn- insert-toimenpide! [db valvonta-id diaarinumero toimenpide-add]
  (first (db/with-db-exception-translation
           jdbc/insert! db :vk-toimenpide
           (assoc toimenpide-add
             :diaarinumero diaarinumero
             :valvonta-id valvonta-id
             :publish-time (Instant/now))
           db/default-opts)))

(defn find-diaarinumero [db id toimenpide]
  (when (or (toimenpide/asha-toimenpide? toimenpide)
            (toimenpide/case-closed? toimenpide))
    (-> (valvonta-kaytto-db/select-last-diaarinumero db {:id id})
        first
        :diaarinumero)))

(defn- find-osapuolet [db valvonta-id]
  (concat
    (find-henkilot db valvonta-id)
    (find-yritykset db valvonta-id)))

(defn- send-toimenpide-email! [db aws-s3-client valvonta toimenpide osapuolet]
  (when (-> db :connection some?)
    (exception/illegal-argument!
      (str "Connections are not thread safe see "
           "https://jdbc.postgresql.org/documentation/head/thread.html. "
           "Existing connection is not allowed when sending emails in background.")))
  (concurrent/run-background
    #(email/send-toimenpide-email! db aws-s3-client valvonta toimenpide osapuolet)
    (str "Sending email failed for toimenpide: " (:id valvonta) "/" (:id toimenpide))))

(defn add-toimenpide! [db aws-s3-client whoami valvonta-id toimenpide-add]
  (jdbc/with-db-transaction
    [tx db]
    (let [valvonta (find-valvonta tx valvonta-id)
          ilmoituspaikat (find-ilmoituspaikat tx)
          diaarinumero (if (toimenpide/case-open? toimenpide-add)
                         (asha/open-case!
                           tx whoami valvonta
                           (find-osapuolet tx valvonta-id)
                           ilmoituspaikat)
                         (find-diaarinumero tx valvonta-id toimenpide-add))
          toimenpide (insert-toimenpide! tx valvonta-id diaarinumero toimenpide-add)
          toimenpide-id (:id toimenpide)]
      (insert-toimenpide-osapuolet! tx valvonta-id toimenpide-id)
      (case (-> toimenpide :type-id toimenpide/type-key)
        :closed (asha/close-case! whoami valvonta-id toimenpide)
        (when (toimenpide/asha-toimenpide? toimenpide)
          (let [find-toimenpide-osapuolet (comp flatten (juxt find-toimenpide-henkilot find-toimenpide-yritykset))
                osapuolet (find-toimenpide-osapuolet tx (:id toimenpide))]
            (asha/log-toimenpide!
              tx aws-s3-client whoami valvonta toimenpide
              osapuolet ilmoituspaikat)
            (send-toimenpide-email! db aws-s3-client valvonta toimenpide osapuolet))))
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
      osapuoli
      (find-osapuolet db id))))


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

(defn find-notes [db id]
  (valvonta-kaytto-db/select-valvonta-notes
    db {:valvonta-id id}))

(defn add-note! [db valvonta-id description]
  (-> (db/with-db-exception-translation
        jdbc/insert! db :vk-note
        {:valvonta-id valvonta-id
         :description description}
        db/default-opts)
      first
      (select-keys [:id])))

(defn update-note! [db id description]
  (first (db/with-db-exception-translation
           jdbc/update! db :vk-note
           {:description description} ["id = ?" id]
           db/default-opts)))
