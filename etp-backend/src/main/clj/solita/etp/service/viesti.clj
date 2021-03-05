(ns solita.etp.service.viesti
  (:require [solita.etp.service.luokittelu :as luokittelu-service]
            [solita.etp.db :as db]
            [clojure.java.jdbc :as jdbc]
            [solita.common.map :as map]
            [solita.etp.service.rooli :as rooli-service]
            [solita.etp.exception :as exception]
            [flathead.flatten :as flat]))

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

(defn add-ketju! [db whoami ketju]
  (assert-vastaanottajaryhma! whoami ketju)
  (db/with-db-exception-translation
    #(jdbc/with-db-transaction
       [tx db]
       (let [[{:keys [id]}] (insert-ketju! tx ketju)]
         (insert-viesti! tx id (:body ketju))
         (when (rooli-service/paakayttaja? whoami)
           (add-vastaanottajat tx id (:vastaanottajat ketju)))
         id))))

(defn- find-viestit [db viestiketju-id]
  (map #(flat/flat->tree #"\$" %)
       (viesti-db/select-viestit db {:id viestiketju-id})))

(defn- assoc-join-viestit [db ketju]
  (assoc ketju :viestit  (find-viestit db (:id ketju))))

(defn- visible-for? [whoami ketju]
  (or (rooli-service/paakayttaja? whoami)
      (contains? (-> ketju :vastaanottajat set) (:id whoami))
      (-> ketju :viestit first :from :id (= (:id whoami)))
      (and (rooli-service/laatija? whoami) (-> ketju :vastaanottajaryhma-id (= 1)))
      (and (rooli-service/laskuttaja? whoami) (-> ketju :vastaanottajaryhma-id (= 2)))))

(defn- assert-visibility [whoami ketju]
  (if (visible-for? whoami ketju) ketju
    (exception/throw-forbidden!
      (str "Kayttaja " (:id whoami)
           " is not allowed to see viestiketju " (:id ketju)))))

(defn find-ketju [db whoami id]
  (->> (viesti-db/select-viestiketju db {:id id})
       (map (partial assoc-join-viestit db))
       (map #(assert-visibility whoami %))
       first))

(defn find-ketjut [db whoami q]
  (let [query (merge {:limit 100 :offset 0} q)]
    (pmap (partial assoc-join-viestit db)
          (cond (rooli-service/paakayttaja? whoami)
                (viesti-db/select-all-viestiketjut db query)
                (rooli-service/laatija? whoami)
                (viesti-db/select-viestiketjut-for-laatija
                  db (assoc query :laatija-id (:id whoami)))
                (rooli-service/laskuttaja? whoami)
                (viesti-db/select-viestiketjut-for-laskuttaja
                  db (assoc query :laskuttaja-id (:id whoami)))
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