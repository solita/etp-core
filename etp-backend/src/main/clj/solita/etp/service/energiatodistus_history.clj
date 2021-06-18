(ns solita.etp.service.energiatodistus-history
  (:require [clojure.string :as str]
            [clojure.data :as data]
            [flathead.flatten :as flat]
            [solita.etp.db :as db]
            [solita.etp.service.energiatodistus :as energiatodistus-service])
  (:import (java.time Instant)))

(db/require-queries 'energiatodistus-history)

(def state-fields #{:tila-id :voimassaolo-paattymisaika :allekirjoitusaika
                    :korvaava-energiatodistus-id})

(defn audit-row->flat-energiatodistus [audit-row]
  (->> audit-row
       energiatodistus-service/db-row->energiatodistus
       flat/sequence->map
       (flat/tree->flat ".")))



(defn audit-event [modifiedby-fullname modifytime k v external-api?]
  (let [type (cond
               (string? v) :str
               (number? v) :number
               (boolean? v) :bool
               (instance? Instant v) :date
               :default :other)]
    {:modifiedby-fullname modifiedby-fullname
     :modifytime (if (and (contains? state-fields k)
                          (= type :date))
                   v
                   modifytime)
     :k k
     :v v
     :type type
     :external-api external-api?}))

(defn audit-history [{:keys [init prev] :as acc}
                     {:keys [modifiedby-fullname modifytime service-uri]
                      :as audit-row}]
  (let [flat-et (audit-row->flat-energiatodistus audit-row)]
    (if init
      (let [[_ diff-to-prev _] (data/diff prev flat-et)
            reverted-keys (-> (data/diff init diff-to-prev) (nth 2) keys)
            new-history (reduce-kv (fn [acc k v]
                                     (assoc acc
                                            k
                                            (audit-event modifiedby-fullname
                                                         modifytime
                                                         k
                                                         v
                                                         false)))
                               {}
                               diff-to-prev)]
        (-> acc
            (assoc :prev flat-et)
            (update :state-history concat (-> new-history
                                              (select-keys state-fields)
                                              vals))
            (update :form-history merge (apply dissoc new-history state-fields))
            (update :form-history #(apply dissoc % reverted-keys))))
      {:init flat-et
       :prev flat-et
       :state-history [(audit-event modifiedby-fullname
                                    modifytime
                                    :tila-id
                                    0
                                    (str/includes? service-uri
                                                   "/api/external/"))]
       :form-history {}})))

(defn find-audit-rows [db id]
  (energiatodistus-history-db/select-energiatodistus-audits db {:id id}))

(defn sort-by-modifytime [coll]
  (sort-by :modifytime coll))

(defn find-history [db id]
  (let [audit-rows (find-audit-rows db id)]
    (as-> audit-rows $
      (reduce audit-history {} $)
      (select-keys $ [:state-history :form-history])
      (update $ :form-history (comp sort-by-modifytime vals))
      (update $ :state-history sort-by-modifytime))))
