(ns solita.etp.service.valvonta-oikeellisuus
  (:require
    [solita.etp.db :as db]
    [solita.etp.service.energiatodistus :as energiatodistus-service]
    [solita.etp.service.valvonta-oikeellisuus.asha :as asha-valvonta-oikeellisuus]
    [solita.etp.service.valvonta-oikeellisuus.toimenpide :as toimenpide]
    [solita.etp.service.valvonta-oikeellisuus.email :as email]
    [clojure.java.jdbc :as jdbc]
    [solita.common.map :as map]
    [solita.etp.service.luokittelu :as luokittelu]
    [solita.etp.service.concurrent :as concurrent]
    [clojure.string :as str]
    [clojure.java.io :as io]
    [solita.etp.service.liite :as liite-service]
    [clojure.set :as set]
    [flathead.flatten :as flat]
    [solita.etp.service.rooli :as rooli-service]
    [solita.etp.exception :as exception]
    [solita.etp.service.viesti :as viesti-service]
    [solita.etp.service.complete-energiatodistus :as complete-energiatodistus-service]

    [solita.common.maybe :as maybe]))

(db/require-queries 'valvonta-oikeellisuus)

(defn- select-keys-prefix [prefix m]
  (into {} (filter (fn [[key _]] (-> key name (str/starts-with? prefix))) m)))

(defn- remove-prefix [m] (map/map-keys #(-> % name (str/replace-first #".*\$" "") keyword) m))

(defn- add-prefix [prefix m] (map/map-keys #(->> % name (str prefix) keyword) m))

(defn- db-row->valvonta [row]
  (let [valvonta (->> row (select-keys-prefix "valvonta$") remove-prefix)
        energiatodistus (energiatodistus-service/db-row->energiatodistus row)
        last-toimenpide  (->> row (select-keys-prefix "last-toimenpide$") remove-prefix)]
    (-> valvonta
        (assoc :id (:id energiatodistus))
        (assoc :last-toimenpide (when-not (-> last-toimenpide :id nil?) last-toimenpide))
        (assoc :energiatodistus energiatodistus))))

(def ^:private default-filters
  {:valvoja-id nil
   :has-valvoja nil
   :include-closed false})

(defn- select-valvonnat [db whoami query paakayttaja-sql laatija-sql]
  (cond
    (rooli-service/paakayttaja? whoami) (paakayttaja-sql db (merge default-filters query))
    (rooli-service/laatija? whoami) (laatija-sql db (assoc query :laatija-id (:id whoami)))
    :else (exception/throw-forbidden! "Allowed only for valvoja or laatija")))

(defn find-valvonnat [db whoami query]
  (->> (select-valvonnat
         db whoami (merge {:limit 10 :offset 0} query)
         valvonta-oikeellisuus-db/select-valvonnat-paakayttaja
         valvonta-oikeellisuus-db/select-valvonnat-laatija)
       (map db-row->valvonta)))

(defn count-valvonnat [db whoami query]
  (-> (select-valvonnat db whoami query
                        valvonta-oikeellisuus-db/count-valvonnat-paakayttaja
                        valvonta-oikeellisuus-db/count-valvonnat-laatija)
      first))

(defn find-valvonta [db id] (first (valvonta-oikeellisuus-db/select-valvonta db {:id id})))

(defn save-valvonta! [db id valvonta]
  (first (db/with-db-exception-translation
           jdbc/update! db :energiatodistus
           (add-prefix "valvonta$" valvonta) ["id = ?" id]
           db/default-opts)))

(defn- insert-toimenpide! [db id diaarinumero toimenpide]
  (first (db/with-db-exception-translation
           jdbc/insert! db :vo-toimenpide
           (assoc toimenpide
             :diaarinumero diaarinumero
             :energiatodistus-id id)
           db/default-opts)))

(defn- insert-virheet! [db toimenpide-id virheet]
  (when-not (empty? virheet)
    (db/with-db-exception-translation
      jdbc/insert-multi! db :vo-virhe
      [:toimenpide-id :type-id :description]
      (map #(vector toimenpide-id (:type-id %) (:description %)) virheet)
      db/default-opts)))

(defn- insert-tiedoksi! [db toimenpide-id tiedoksi]
  (when-not (empty? tiedoksi)
    (db/with-db-exception-translation
      jdbc/insert-multi! db :vo-tiedoksi
      [:toimenpide-id :name :email]
      (map #(vector toimenpide-id (:name %) (:email %)) tiedoksi)
      db/default-opts)))

(defn find-diaarinumero [db id toimenpide]
  (when (or (toimenpide/asha-toimenpide? toimenpide)
            (toimenpide/case-closed? toimenpide))
    (-> (valvonta-oikeellisuus-db/select-last-diaarinumero db {:id id})
        first :diaarinumero)))

(defn add-anomaly-viestiketju! [db whoami id toimenpide]
  (let [energiatodistus (complete-energiatodistus-service/find-complete-energiatodistus db id)]
    (viesti-service/add-ketju!
      db whoami
      {:vastaanottajat        [(:laatija-id energiatodistus)]
       :vastaanottajaryhma-id nil
       :energiatodistus-id    id
       :vo-toimenpide-id      (:id toimenpide)
       :kasitelty             true
       :kasittelija-id        (:id whoami)
       :subject               (str "Poikkeamailmoitus ET " (:energiatodistus-id toimenpide))
       :body
       (str (-> energiatodistus :perustiedot :nimi) "\n"
            (or (-> energiatodistus :perustiedot :katuosoite-fi)
                (-> energiatodistus :perustiedot :katuosoite-sv)) "\n"
            (-> energiatodistus :perustiedot :postinumero) " "
            (-> energiatodistus :perustiedot :postitoimipaikka-fi) "\n\n"
            (:description toimenpide) "\n\n"
            "Energia-asiantuntija\n"
            (:etunimi whoami) " " (:sukunimi whoami))})))

(defn add-audit-reply-viestiketju! [db whoami id toimenpide]
  (viesti-service/add-ketju!
    db whoami
    {:vastaanottajat        []
     :vastaanottajaryhma-id 0
     :energiatodistus-id    id
     :vo-toimenpide-id      (:id toimenpide)
     :subject               (str "Valvontamuistio ET " (:energiatodistus-id toimenpide))
     :body                  (str (:description toimenpide) "\n\n"
                                 "Katso vastauksen liitteet energiatodistuksen valvontavälilehdeltä.")}))

(defn- send-toimenpide-email! [db aws-s3-client id toimenpide]
  (when (-> db :connection some?)
    (exception/illegal-argument!
      (str "Connections are not thread safe see "
           "https://jdbc.postgresql.org/documentation/head/thread.html. "
           "Existing connection is not allowed when sending emails in background.")))
  (concurrent/run-background
    #(email/send-toimenpide-email! db aws-s3-client id toimenpide)
    (str "Sending email failed for toimenpide: " id "/" (:id toimenpide))))

(defn add-toimenpide! [db aws-s3-client whoami id toimenpide-add]
  (jdbc/with-db-transaction [tx db]
    (let [diaarinumero (if (toimenpide/case-open? toimenpide-add)
                         (asha-valvonta-oikeellisuus/open-case! tx whoami id)
                         (find-diaarinumero tx id toimenpide-add))
          toimenpide (insert-toimenpide! tx id diaarinumero (dissoc toimenpide-add :virheet :tiedoksi))
          toimenpide-id (:id toimenpide)]
        (insert-virheet! tx toimenpide-id (:virheet toimenpide-add))
        (insert-tiedoksi! tx toimenpide-id (:tiedoksi toimenpide-add))
        (when-not (toimenpide/draft-support? toimenpide)
          (valvonta-oikeellisuus-db/update-toimenpide-published! tx {:id toimenpide-id})
          (case (-> toimenpide :type-id toimenpide/type-key)
            :closed (asha-valvonta-oikeellisuus/close-case! whoami id toimenpide)
            (when (toimenpide/asha-toimenpide? toimenpide)
              (asha-valvonta-oikeellisuus/log-toimenpide! tx aws-s3-client whoami id toimenpide)))
          (send-toimenpide-email! db aws-s3-client id toimenpide))
        (when (toimenpide/anomaly? toimenpide)
          (add-anomaly-viestiketju! tx whoami id toimenpide))
        (when (toimenpide/clears-from-tyojono? toimenpide)
          (save-valvonta! tx id {:pending false}))
        {:id toimenpide-id})))

(defn- assoc-virheet [db toimenpide]
  (assoc toimenpide :virheet (valvonta-oikeellisuus-db/select-toimenpide-virheet
                               db {:toimenpide-id (:id toimenpide)})))

(defn- assoc-tiedoksi [db toimenpide]
  (assoc toimenpide :tiedoksi (valvonta-oikeellisuus-db/select-toimenpide-tiedoksi
                                db {:toimenpide-id (:id toimenpide)})))

(defn toimenpide-filename [{:keys [type-id]}]
  (-> type-id
      asha-valvonta-oikeellisuus/toimenpide-type->document
      :filename))

(defn- db-row->toimenpide [db-row]
  (as-> db-row %
      (flat/flat->tree #"\$" %)
      (assoc % :filename (toimenpide-filename %))))

(defn find-toimenpiteet [db whoami id]
  (when-not
    ;; assert privileges to view et information and check that it exists
    (nil? (energiatodistus-service/find-energiatodistus db whoami id))
    (map db-row->toimenpide
         (valvonta-oikeellisuus-db/select-toimenpiteet
           db {:energiatodistus-id id
               :paakayttaja?       (rooli-service/paakayttaja? whoami)}))))

(defn find-toimenpide [db whoami id toimenpide-id]
  ;; assert privileges to view et information:
  (energiatodistus-service/find-energiatodistus db whoami id)
  (->> (valvonta-oikeellisuus-db/select-toimenpide db {:id toimenpide-id})
       (map (partial assoc-virheet db))
       (map (partial assoc-tiedoksi db))
       (map db-row->toimenpide)
       first))

(defn find-toimenpide-liitteet [db whoami id toimenpide-id]
  ;; assert privileges to view et information:
  (when-not (nil? (energiatodistus-service/find-energiatodistus db whoami id))
    (valvonta-oikeellisuus-db/select-toimenpide-liitteet db {:toimenpide-id toimenpide-id})))

(defn add-liitteet-from-files! [db aws-s3-client whoami id toimenpide-id files]
  (jdbc/with-db-transaction
    [db db]
    (mapv #(liite-service/add-liite-from-file!
             db aws-s3-client id
             (-> %
                 (assoc :vo-toimenpide-id toimenpide-id)
                 (set/rename-keys {:content-type :contenttype
                                   :filename     :nimi})))
          files)))

(defn add-liite-from-link! [db whoami id toimenpide-id liite]
  (liite-service/add-liite-from-link!
    db id (assoc liite :vo-toimenpide-id toimenpide-id)))

(defn delete-liite! [db whoami id toimenpide-id liite-id]
  (liite-service/delete-liite! db liite-id))

(defn- update-toimenpide-row! [db toimenpide-id toimenpide]
  (first (db/with-db-exception-translation
           jdbc/update! db :vo-toimenpide
           toimenpide ["id = ?" toimenpide-id]
           db/default-opts)))

(defn update-toimenpide! [db whoami id toimenpide-id toimenpide-update]
  (jdbc/with-db-transaction [db db]
    (when (toimenpide/audit-report? (find-toimenpide db whoami id toimenpide-id ))
      (valvonta-oikeellisuus-db/delete-toimenpide-virheet! db {:toimenpide-id toimenpide-id})
      (insert-virheet! db toimenpide-id (:virheet toimenpide-update))

      (valvonta-oikeellisuus-db/delete-toimenpide-tiedoksi! db {:toimenpide-id toimenpide-id})
      (insert-tiedoksi! db toimenpide-id (:tiedoksi toimenpide-update)))
    (update-toimenpide-row! db toimenpide-id (dissoc toimenpide-update :virheet :tiedoksi))))

(defn publish-toimenpide! [db aws-s3-client whoami id toimenpide-id]
  (when-let [toimenpide (find-toimenpide db whoami id toimenpide-id)]
    (jdbc/with-db-transaction [tx db]
      (when (toimenpide/asha-toimenpide? toimenpide)
        (asha-valvonta-oikeellisuus/log-toimenpide! tx aws-s3-client whoami id toimenpide))
      (when (toimenpide/audit-reply? toimenpide)
        (add-audit-reply-viestiketju! tx whoami id toimenpide))
      (send-toimenpide-email! db aws-s3-client id toimenpide)
      (valvonta-oikeellisuus-db/update-toimenpide-published! tx {:id toimenpide-id}))))

(defn find-toimenpidetyypit [db] (luokittelu/find-toimenpidetypes db))

(defn find-templates [db] (valvonta-oikeellisuus-db/select-templates db))

(defn find-virhetyypit [db] (valvonta-oikeellisuus-db/select-virhetypes db))

(defn save-virhetyyppi! [db id virhetyyppi]
  (first (db/with-db-exception-translation
           jdbc/update! db :vo-virhetype
           virhetyyppi ["id = ?" id]
           db/default-opts)))

(defn add-virhetyyppi! [db virhetyyppi]
  (valvonta-oikeellisuus-db/insert-virhetype<! db virhetyyppi))

(defn find-severities [db] (luokittelu/find-severities db))

(defn preview-toimenpide [db whoami id toimenpide]
  (maybe/map*
    io/input-stream
    (asha-valvonta-oikeellisuus/generate-pdf-document
        db whoami
        (assoc toimenpide :diaarinumero (find-diaarinumero db id toimenpide))
        id)))

(defn find-toimenpide-document [db aws-s3-client whoami id toimenpide-id]
  (when-let [toimenpide (find-toimenpide db whoami id toimenpide-id)]
    (if (:publish-time toimenpide)
      (asha-valvonta-oikeellisuus/find-document aws-s3-client id toimenpide-id)
      (preview-toimenpide db whoami id toimenpide))))

(defn find-notes [db id] (valvonta-oikeellisuus-db/select-valvonta-notes
                           db {:energiatodistus-id id}))

(defn add-note! [db id description]
  (-> (db/with-db-exception-translation
        jdbc/insert! db :vo-note
        {:energiatodistus-id id
         :description        description}
        db/default-opts)
      first
      (select-keys [:id])))

(defn update-note! [db id description]
  (first (db/with-db-exception-translation
           jdbc/update! db :vo-note
           {:description description} ["id = ?" id]
           db/default-opts)))
