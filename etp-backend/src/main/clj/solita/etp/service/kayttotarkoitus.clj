(ns solita.etp.service.kayttotarkoitus
  (:require [solita.etp.db :as db]))

; *** Require sql functions ***
(db/require-queries 'kayttotarkoitus)

(defn find-kayttotarkoitukset [db versio]
  (kayttotarkoitus-db/select-kayttotarkoitusluokat-by-versio
   db
   {:versio versio}))

(defn find-alakayttotarkoitukset [db versio]
  (kayttotarkoitus-db/select-alakayttotarkoitusluokat-by-versio
   db
   {:versio versio}))

(defn find-kayttotarkoitus-by-alakayttotarkoitus-id [db versio id]
  (-> db
      (kayttotarkoitus-db/select-kayttotarkoitusluokka-by-versio-and-alakayttotarkoitusluokka-id
       {:versio versio
        :id id})
      first))
