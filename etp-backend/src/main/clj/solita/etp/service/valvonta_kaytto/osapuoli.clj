(ns solita.etp.service.valvonta-kaytto.osapuoli)

(def omistaja? #(= (:rooli-id %) 0))
(def tiedoksi? (complement omistaja?))
(def other-rooli? #(= (:rooli-id %) 2))

(defn- toimitustapa? [toimitustapa-id] #(= (:toimitustapa-id %) toimitustapa-id))

(def suomi-fi? (toimitustapa? 0))
(def email? (toimitustapa? 1))


(def henkilo? #(and (contains? % :etunimi) (contains? % :sukunimi)))
(def yritys? #(contains? % :nimi))

(defn ilmoituspaikka-other? [valvonta]
  (= (:ilmoituspaikka-id valvonta) 2))