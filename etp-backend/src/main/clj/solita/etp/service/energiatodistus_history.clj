(ns solita.etp.service.energiatodistus-history
  (:require [clojure.data :as data]
            [flathead.flatten :as flat]
            [solita.etp.db :as db]
            [solita.etp.service.energiatodistus :as energiatodistus-service]))

(db/require-queries 'energiatodistus-history)

(def state-fields #{:tila-id :voimassaolo-paattymisaika :allekirjoitusaika
                    :korvaava-energiatodistus-id})

(defn audit-row->flat-energiatodistus [audit-row]
  (->> audit-row
       energiatodistus-service/db-row->energiatodistus
       flat/sequence->map
       (flat/tree->flat "$")))

(defn audit-event [modifiedby-fullname modifytime k v]
  {:modifiedby-fullname modifiedby-fullname
   :modifytime modifytime
   :k k
   :v v})

(defn audit-history [{:keys [init prev] :as acc}
                      {:keys [modifiedby-fullname modifytime] :as audit-row}]
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
                                                         v)))
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
       :state-history [(audit-event modifiedby-fullname modifytime :tila-id 0)]
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
