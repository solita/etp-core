(ns solita.etp.service.valvonta-kaytto.toimenpide
  (:require [solita.etp.db :as db]))

(db/require-queries 'valvonta-kaytto)

(def ^:private type-id->type-key
  {;; käytönvalvonnan asia avataan ashaan (case open)
   0 :case
   ;; tietopyynnön toimenpidetyypit
   1 :rfi-request
   2 :rfi-order
   3 :rfi-warning
   ;; päätös
   4 :decision-order
   ;; valvonnan sulkeminen (case closed)
   5 :closed
   ;; Uhkasakkoprosessi
   6 :court-hearing
   7 :decision-order-hearing-letter
   8 :decision-order-actual-decision})

(defn type-key [type-id]
  (if-let [type-key (type-id->type-key type-id)]
    type-key
    (throw (Exception.))))

(defn type? [type toimenpide]
  (= (-> toimenpide :type-id type-key) type))

(defn some-type? [type-key-set toimenpide]
  (contains? type-key-set (-> toimenpide :type-id type-key)))

(def case-open? (partial type? :case))
(def case-close? (partial type? :closed))
(def send-tiedoksi? (partial type? :rfi-request))

(def kaskypaatos-kuulemiskirje? (partial type? :decision-order-hearing-letter))

(def kaskypaatos-varsinainen-paatos? (partial type? :decision-order-actual-decision))

(def kaskypaatos-toimenpide?
  (partial some-type? #{:decision-order-hearing-letter
                        :decision-order-actual-decision}))

(def asha-toimenpide?
  (partial some-type? #{:rfi-request :rfi-order :rfi-warning :decision-order-hearing-letter :decision-order-actual-decision}))


(def with-diaarinumero? (comp not (partial type? :case)))

(defn manually-deliverable? [db type-id]
  (let [manually-deliverable-toimenpidetypes
        (->> db
             valvonta-kaytto-db/select-manually-deliverable-toimenpidetypes
             (map :id)
             set)]
    (contains? manually-deliverable-toimenpidetypes type-id)))
