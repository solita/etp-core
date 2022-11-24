(ns solita.etp.service.aineisto
  (:require
   [solita.etp.db :as db]))

(db/require-queries 'aineisto)

(def ^:private aineisto-keys
  [:banks :tilastokeskus :anonymized-set])

(defn set-access! [db kayttaja-id {:keys [aineisto-id valid-until ip-address]}]
  (aineisto-db/insert-kayttaja-aineisto! db {:aineisto-id aineisto-id
                                             :kayttaja-id kayttaja-id
                                             :valid-until valid-until
                                             :ip-address ip-address}))

(defn delete-access! [db kayttaja-id aineisto-id]
  (aineisto-db/delete-kayttaja-aineisto! db {:kayttaja-id kayttaja-id
                                             :aineisto-id aineisto-id}))


(defn check-access [db kayttaja-id kayttaja-ip aineisto-id]
  (contains? (into #{} (->> (aineisto-db/select-kayttaja-aineistot db {:kayttaja-id kayttaja-id
                                                                       :ip-address kayttaja-ip})
                            (map :aineisto-id)))
             aineisto-id))
