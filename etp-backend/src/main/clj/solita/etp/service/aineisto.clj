(ns solita.etp.service.aineisto
  (:require
   [clojure.java.jdbc :as jdbc]
   [solita.etp.db :as db]
   [solita.etp.service.luokittelu :as luokittelu-service]))

(db/require-queries 'aineisto)

(def ^:private aineisto-keys
  [:banks :tilastokeskus :anonymized-set])

(defn aineisto-key [aineisto-id]
  (->> aineisto-id dec (nth aineisto-keys)))

(def find-aineistot luokittelu-service/find-aineistot)

(defn set-access! [db kayttaja-id {:keys [aineisto-id valid-until ip-address]}]
  (aineisto-db/insert-kayttaja-aineisto! db {:aineisto-id aineisto-id
                                             :kayttaja-id kayttaja-id
                                             :valid-until valid-until
                                             :ip-address ip-address}))

(defn delete-kayttaja-access! [db kayttaja-id]
  (aineisto-db/delete-kayttaja-access! db {:kayttaja-id kayttaja-id}))

(defn delete-access! [db kayttaja-id aineisto-id]
  (aineisto-db/delete-kayttaja-aineisto! db {:kayttaja-id kayttaja-id
                                             :aineisto-id aineisto-id}))


(defn check-access [db kayttaja-id aineisto-id ip-address]
  (contains? (into #{} (->> (aineisto-db/select-allowed-aineisto-id-for-ip db {:kayttaja-id kayttaja-id
                                                                               :ip-address  ip-address})
                            (map :aineisto-id)))
             aineisto-id))

(defn find-kayttaja-aineistot [db kayttaja-id]
  (aineisto-db/select-all-kayttaja-aineistot db {:kayttaja-id kayttaja-id}))

(defn set-kayttaja-aineistot! [db kayttaja-id aineistot]
  ;; Only 10 ip addresses are allowed to access the aineistot
  (when (> (count aineistot) 10)
    (throw (ex-info "Maximum of 10 permits allowed"
                    {:type :too-many-permits})))

  (jdbc/with-db-transaction [db db]
    ;; Clear out any previously existing entries from the user
    (let [_ (aineisto-db/delete-kayttaja-access! db {:kayttaja-id kayttaja-id})]
      ;; Fill in new ones
      (doseq [aineisto aineistot]
        (set-access! db kayttaja-id aineisto))
      1)))
