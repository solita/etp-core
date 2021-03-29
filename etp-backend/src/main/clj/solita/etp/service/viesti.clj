(ns solita.etp.service.viesti
  (:require [clojure.java.jdbc :as jdbc]
            [flathead.flatten :as flat]
            [solita.common.map :as map]
            [solita.etp.db :as db]
            [solita.etp.exception :as exception]
            [solita.etp.service.luokittelu :as luokittelu-service]
            [solita.etp.service.rooli :as rooli-service]))

(db/require-queries 'viesti)

(defn- add-vastaanottajat [db viestiketju-id vastaanottajat]
  (doseq [vastaanottaja-id vastaanottajat]
    (jdbc/insert! db :vastaanottaja
                  (map/bindings->map viestiketju-id vastaanottaja-id)
                  db/default-opts)))

(defn- insert-ketju! [db ketju]
  (jdbc/insert! db :viestiketju
                (select-keys ketju [:vastaanottajaryhma-id :energiatodistus-id :subject])
                db/default-opts))

(defn- insert-viesti! [db viestiketju-id body]
  (jdbc/insert! db :viesti
                (map/bindings->map viestiketju-id body)
                db/default-opts))

(defn- assert-vastaanottajaryhma! [whoami ketju]
  (when (and (rooli-service/laatija? whoami)
             (not= 0 (:vastaanottajaryhma-id ketju)))
    (exception/throw-forbidden!
      (str "Laatija " (:id whoami)
           " is not allowed to use vastaanottajaryhma: "
           (:vastaanottajaryhma-id ketju)))))

(defn- assert-vastaanottajaryhma-or-vastaanottaja-exists! [ketju]
  (when (and (-> ketju :vastaanottajat empty?)
             (-> ketju :vastaanottajaryhma-id nil?))
    (exception/throw-ex-info!
     {:type :missing-vastaanottaja-or-vastaanottajaryhma-id
      :message "Missing either vastaanottaja or vastaanottajaryhma-id"})))

(defn add-ketju! [db whoami ketju]
  (assert-vastaanottajaryhma! whoami ketju)
  (assert-vastaanottajaryhma-or-vastaanottaja-exists! ketju)
  (db/with-db-exception-translation
    #(jdbc/with-db-transaction
       [tx db]
       (let [[{:keys [id]}] (insert-ketju! tx ketju)]
         (insert-viesti! tx id (:body ketju))
         (when (or (rooli-service/paakayttaja? whoami)
                   (rooli-service/laskuttaja? whoami))
           (add-vastaanottajat tx id (:vastaanottajat ketju)))
         id))))

(defn update-ketju! [db id ketju-edit]
  (first (db/with-db-exception-translation
          jdbc/update! db :viestiketju ketju-edit ["id = ?" id] db/default-opts)))

(defn- find-viestit [db viestiketju-id]
  (map #(flat/flat->tree #"\$" %)
       (viesti-db/select-viestit db {:id viestiketju-id})))

(defn find-kayttajat [db]
  (->> db viesti-db/select-kayttajat (group-by :id) (map/map-values first)))

(defn- assoc-join-viestit [db ketju]
  (assoc ketju :viestit  (find-viestit db (:id ketju))))

(defn- assoc-join-vastaanottajat [kayttajat ketju]
  (->> ketju
       :vastaanottajat
       (map kayttajat (:vastaanottajat ketju))
       (assoc ketju :vastaanottajat)))

(defn- visible-for? [whoami ketju]
  (or (rooli-service/paakayttaja? whoami)
      (contains? (->> ketju :vastaanottajat (map :id) set) (:id whoami))
      (-> ketju :viestit first :from :id (= (:id whoami)))
      (and (rooli-service/laatija? whoami) (-> ketju :vastaanottajaryhma-id (= 1)))
      (and (rooli-service/laskuttaja? whoami) (-> ketju :vastaanottajaryhma-id (= 2)))))

(defn- assert-visibility [whoami ketju]
  (if (visible-for? whoami ketju) ketju
    (exception/throw-forbidden!
      (str "Kayttaja " (:id whoami)
           " is not allowed to see viestiketju " (:id ketju)))))

(defn find-ketju [db whoami id]
  (let [kayttajat (find-kayttajat db)]
    (->> (viesti-db/select-viestiketju db {:id id})
         (map (comp (partial assoc-join-viestit db)
                 (partial assoc-join-vastaanottajat kayttajat)))
         (map #(assert-visibility whoami %))
         first)))

(defn find-ketjut [db whoami q]
  (let [query (merge {:limit 100 :offset 0} q)
        kayttajat (find-kayttajat db)]
    (pmap (comp (partial assoc-join-viestit db)
             (partial assoc-join-vastaanottajat kayttajat))
          (cond (rooli-service/paakayttaja? whoami)
                (viesti-db/select-all-viestiketjut db query)

                (rooli-service/laatija? whoami)
                (viesti-db/select-viestiketjut-for-kayttaja
                 db
                 (assoc query :kayttaja-id (:id whoami) :vastaanottajaryhma-id 1))

                (rooli-service/laskuttaja? whoami)
                (viesti-db/select-viestiketjut-for-kayttaja
                 db
                 (assoc query :kayttaja-id (:id whoami) :vastaanottajaryhma-id 2))

                :else []))))

(defn count-ketjut [db whoami]
  (-> (cond (rooli-service/paakayttaja? whoami)
            (viesti-db/select-count-all-viestiketjut db)
            (rooli-service/laatija? whoami)
            (viesti-db/select-count-viestiketjut-for-laatija
              db {:laatija-id (:id whoami)})
            :else [{:count 0}])
      first))

(defn add-viesti! [db whoami id body]
  (when (find-ketju db whoami id)
    (insert-viesti! db id body)))

(defn find-vastaanottajaryhmat [db]
  (luokittelu-service/find-vastaanottajaryhmat db))

(defn find-kasittelijat [db]
  (viesti-db/select-kasittelijat db))
