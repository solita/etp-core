(ns solita.etp.service.toimenpide)

(def ^:private type-keys
  [;; kevyt valvontamenettely
   :verified :anomaly

   ;; raskas valvontamenettely
   ;; asia avataan ashaan (case open)
   :case
   ;; tietopyynnön toimenpidetyypit
   :rfi-request :rfi-reply :rfi-order :rfi-warning
   ;; valvonnan toimenpidetyypit
   :audit-report :audit-reply :audit-order :audit-warning
   ;; päätös
   :decision-prohibition
   ;; raskaan valvonnan sulkeminen (case closed)
   :closed])

(defn type-key [type-id] (nth type-keys type-id))

(defn type? [type toimenpide]
  (= (-> toimenpide :type-id type-key) type))

(def draft-support? (partial type? :audit-report))

(def case-open? (partial type? :case))
