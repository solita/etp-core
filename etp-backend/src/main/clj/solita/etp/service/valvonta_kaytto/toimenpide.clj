(ns solita.etp.service.valvonta-kaytto.toimenpide)

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
   7 :decision-order-hearing-letter})

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

(def asha-toimenpide?
  (partial some-type? #{:rfi-request :rfi-order :rfi-warning}))

(def with-diaarinumero? (comp not (partial type? :case)))
