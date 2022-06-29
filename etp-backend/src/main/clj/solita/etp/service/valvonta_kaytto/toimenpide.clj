(ns solita.etp.service.valvonta-kaytto.toimenpide)

(def ^:private type-keys
  [;; käytönvalvonnan asia avataan ashaan (case open)
   :case
   ;; tietopyynnön toimenpidetyypit
   :rfi-request :rfi-order :rfi-warning
   ;; päätös
   :decision-order
   ;; valvonnan sulkeminen (case closed)
   :closed])

(defn type-key [type-id] (nth type-keys type-id))

(defn type? [type toimenpide]
  (= (-> toimenpide :type-id type-key) type))

(defn some-type? [type-key-set toimenpide]
  (contains? type-key-set (-> toimenpide :type-id type-key)))

(def case-open? (partial type? :case))
(def send-tiedoksi? (partial type? :rfi-request))

(def asha-toimenpide?
  (partial some-type? #{:rfi-request :rfi-order :rfi-warning}))

(def with-diaarinumero? (comp not (partial type? :case)))
