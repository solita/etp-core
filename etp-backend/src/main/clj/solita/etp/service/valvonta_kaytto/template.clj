(ns solita.etp.service.valvonta-kaytto.template)

(defn send-tiedoksi? [template]
  (println template)
  (:tiedoksi template))
