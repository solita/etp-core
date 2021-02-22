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

(defn find-ketju [db id]
  (->> (viesti-db/select-viestiketju db {:id id})
       (map (partial assoc-join-viestit db))
       first))

(defn find-ketjut [db whoami]
  (pmap (partial assoc-join-viestit db)
        (cond (rooli-service/paakayttaja? whoami)
              (viesti-db/select-all-viestiketjut db)
              (rooli-service/laatija? whoami)
              (viesti-db/select-viestiketjut-for-laatija db {:laatija-id (:id whoami)})
              :else [])))

(defn count-ketjut [db] {:count (count @ketjut)})

(defn add-viesti! [db whoami id body]
  (insert-viesti! db id body))

(defn find-vastaanottajaryhmat [db]
  (luokittelu-service/find-vastaanottajaryhmat db))