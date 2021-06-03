(ns solita.etp.service.valvonta-oikeellisuus
  (:require
    [solita.etp.db :as db]
    [solita.etp.schema.energiatodistus :as energiatodistus-schema]
    [solita.etp.service.energiatodistus :as energiatodistus-service]
    [solita.etp.service.asha-valvonta-oikeellisuus :as asha-valvonta-oikeellisuus]
    [solita.etp.service.toimenpide :as toimenpide]
    [solita.etp.service.pdf :as pdf]
    [clojure.java.jdbc :as jdbc]
    [solita.common.map :as map]
    [solita.etp.service.luokittelu :as luokittelu]
    [clojure.string :as str]
    [clojure.java.io :as io]))

(db/require-queries 'valvonta-oikeellisuus)

(def ^:private db-row->energiatodistus
  (energiatodistus-service/schema->db-row->energiatodistus
    energiatodistus-schema/Energiatodistus))

(defn- select-keys-prefix [prefix m]
  (into {} (filter (fn [[key _]] (-> key name (str/starts-with? prefix))) m)))

(defn- remove-prefix [m] (map/map-keys #(-> % name (str/replace-first #".*\$" "") keyword) m))

(defn- add-prefix [prefix m] (map/map-keys #(->> % name (str prefix) keyword) m))

(defn- db-row->valvonta [row]
  (let [valvonta (->> row (select-keys-prefix "valvonta$") remove-prefix)
        energiatodistus (db-row->energiatodistus row)
        last-toimenpide  (->> row (select-keys-prefix "last-toimenpide$") remove-prefix)]
    (-> valvonta
        (assoc :id (:id energiatodistus))
        (assoc :last-toimenpide (when-not (-> last-toimenpide :id nil?) last-toimenpide))
        (assoc :energiatodistus energiatodistus))))

(defn find-valvonnat [db query]
  (map db-row->valvonta (valvonta-oikeellisuus-db/select-valvonnat db (merge {:limit 10 :offset 0} query))))

(defn count-valvonnat [db] (first (valvonta-oikeellisuus-db/count-valvonnat db)))

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

(defn find-diaarinumero [db id toimenpide]
  (when (or (toimenpide/asha-toimenpide? toimenpide)
            (toimenpide/case-closed? toimenpide))
    (-> (valvonta-oikeellisuus-db/select-last-diaarinumero db {:id id})
        first :diaarinumero)))

(defn add-toimenpide! [db aws-s3-client whoami id toimenpide-add]
  (let [diaarinumero (if (toimenpide/case-open? toimenpide-add)
                       (asha-valvonta-oikeellisuus/open-case! db whoami id)
                       (find-diaarinumero db id toimenpide-add))
        toimenpide (insert-toimenpide! db id diaarinumero (dissoc toimenpide-add :virheet))
        toimenpide-id (:id toimenpide)]
    (jdbc/with-db-transaction [db db]
      (insert-virheet! db toimenpide-id (:virheet toimenpide-add))
      (when-not (toimenpide/draft-support? toimenpide)
        (valvonta-oikeellisuus-db/update-toimenpide-published! db {:id toimenpide-id})
        (case (-> toimenpide :type-id toimenpide/type-key)
          :closed (asha-valvonta-oikeellisuus/close-case! whoami id toimenpide)
          (when (toimenpide/asha-toimenpide? toimenpide)
            (asha-valvonta-oikeellisuus/log-toimenpide! db aws-s3-client whoami id toimenpide))))
      {:id toimenpide-id})))

(defn- assoc-virheet [db toimenpide]
  (assoc toimenpide :virheet (valvonta-oikeellisuus-db/select-toimenpide-virheet
                               db {:toimenpide-id (:id toimenpide)})))

(defn- ->toimenpide-filename [toimenpide]
  (let [{:keys [filename]} (and (:template-id toimenpide) (asha-valvonta-oikeellisuus/toimenpide-type->document (:type-id toimenpide)))]
    (assoc toimenpide :filename filename)))

(defn find-toimenpiteet [db whoami id]
  (when-not
    ;; assert privileges to view et information and check that it exists
    (nil? (energiatodistus-service/find-energiatodistus db whoami id))
    (->> (valvonta-oikeellisuus-db/select-toimenpiteet db {:energiatodistus-id id})
         (map (partial ->toimenpide-filename)))))

(defn find-toimenpide [db whoami id toimenpide-id]
  ;; assert privileges to view et information:
  (energiatodistus-service/find-energiatodistus db whoami id)
  (->> (valvonta-oikeellisuus-db/select-toimenpide db {:id toimenpide-id})
       (map (partial assoc-virheet db))
       (map (partial ->toimenpide-filename))
       first))

(defn- update-toimenpide-row! [db toimenpide-id toimenpide]
  (first (db/with-db-exception-translation
           jdbc/update! db :vo-toimenpide
           toimenpide ["id = ?" toimenpide-id]
           db/default-opts)))

(defn update-toimenpide! [db whoami id toimenpide-id toimenpide-update]
  (jdbc/with-db-transaction [db db]
    (when (toimenpide/audit-report? (find-toimenpide db whoami id toimenpide-id ))
      (valvonta-oikeellisuus-db/delete-toimenpide-virheet! db {:toimenpide-id toimenpide-id})
      (insert-virheet! db toimenpide-id (:virheet toimenpide-update)))
    (update-toimenpide-row! db toimenpide-id (dissoc toimenpide-update :virheet))))

(defn publish-toimenpide! [db aws-s3-client whoami id toimenpide-id]
  (let [toimenpide (find-toimenpide db whoami id toimenpide-id)]
    (when (toimenpide/asha-toimenpide? toimenpide)
      (asha-valvonta-oikeellisuus/log-toimenpide! db aws-s3-client whoami id toimenpide)))
  (valvonta-oikeellisuus-db/update-toimenpide-published! db {:id toimenpide-id}))

(defn find-toimenpidetyypit [db] (luokittelu/find-toimenpidetypes db))

(defn find-templates [db] (valvonta-oikeellisuus-db/select-templates db))

(defn find-virhetyypit [db] (valvonta-oikeellisuus-db/select-virhetypes db))

(defn find-severities [db] (luokittelu/find-severities db))

(defn generate-template [db whoami id toimenpide]
  (let [{:keys [energiatodistus laatija]} (asha-valvonta-oikeellisuus/resolve-energiatodistus-laatija db id)
        diaarinumero (find-diaarinumero db id toimenpide)
        toimenpide-with-diaarinumero (assoc toimenpide :diaarinumero diaarinumero)]
    (asha-valvonta-oikeellisuus/generate-template whoami toimenpide-with-diaarinumero energiatodistus laatija)))

(defn preview-toimenpide [db whoami id toimenpide ostream]
  (when-let [{:keys [template template-data]} (generate-template db whoami id toimenpide)]
    (with-open [output (io/output-stream ostream)]
      (pdf/html->pdf template template-data output))))

(defn find-toimenpide-document [db aws-s3-client whoami id toimenpide-id ostream]
  (when-let [toimenpide (find-toimenpide db whoami id toimenpide-id)]
    (if (:publish-time toimenpide)
      (with-open [output (io/output-stream ostream)]
        (io/copy (asha-valvonta-oikeellisuus/find-document aws-s3-client id toimenpide-id) output))
      (preview-toimenpide db whoami id toimenpide ostream))))
