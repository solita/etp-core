(ns solita.etp.service.aineisto
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.network.ip :as ip]
    [clojure.tools.logging :as log]
    [solita.etp.db :as db]
    [solita.etp.service.luokittelu :as luokittelu-service])
  (:import (java.time Instant)))

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
                                             :ip-address  ip-address}))

(defn delete-kayttaja-access! [db kayttaja-id]
  (aineisto-db/delete-kayttaja-access! db {:kayttaja-id kayttaja-id}))

(defn delete-access! [db kayttaja-id aineisto-id]
  (aineisto-db/delete-kayttaja-aineisto! db {:kayttaja-id kayttaja-id
                                             :aineisto-id aineisto-id}))

(defn- ip-matches? [request-ip allowed-network-or-ip]
  (if (re-find #"/" allowed-network-or-ip)
    (let [ip-address (ip/make-ip-address request-ip)
          network (ip/make-network allowed-network-or-ip)]
      (contains? network ip-address))
    (= request-ip allowed-network-or-ip)))

(defn find-kayttaja-aineistot [db kayttaja-id]
  (aineisto-db/select-all-kayttaja-aineistot db {:kayttaja-id kayttaja-id}))

(defn check-access [db kayttaja-id aineisto-id ip-address]
  (let [all-aineistot (find-kayttaja-aineistot db kayttaja-id)
        wanted-aineistot (filter #(= aineisto-id (:aineisto-id %)) all-aineistot)
        correct-ip (filter #(ip-matches? ip-address (:ip-address %)) wanted-aineistot)
        is-valid-now (filter #(.isAfter (:valid-until %) (Instant/now)) correct-ip)]
    (cond
      (empty? all-aineistot) (do (log/info (str "User " kayttaja-id " has no access to any aineisto")) false)
      (empty? wanted-aineistot) (do (log/info (str "User " kayttaja-id "has no access to aineisto " aineisto-id)) false)
      (empty? correct-ip) (do (log/info (str "User " kayttaja-id " has no access to aineisto " aineisto-id " from IP " ip-address)) false)
      (empty? is-valid-now) (do (log/info (str "User " kayttaja-id " access to aineisto " aineisto-id " has expired")) false)
      :else true)))

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
