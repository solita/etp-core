(ns solita.etp.service.asha-valvonta-oikeellisuus
  (:require [solita.etp.service.asha :as asha]
            [solita.etp.service.energiatodistus :as energiatodistus-service]
            [solita.etp.service.kayttaja :as kayttaja-service]
            [clojure.string :as str]))

(defn create-case [whoami db toimenpide]
  (when-let [energiatodistus (energiatodistus-service/find-energiatodistus-any-laatija db (:energiatodistus-id toimenpide))]
    (let [laatija (kayttaja-service/find-kayttaja db (:laatija-id energiatodistus))]
      (asha/case-create {:request-id     (:email whoami)
                         :sender-id      (str (:id energiatodistus) "/" (:id toimenpide))
                         :classification "05.03.02"
                         :service        "general"          ; Yleinen menettely
                         :name           (str/join ", " [(-> energiatodistus :perustiedot :katuosoite-fi) (:laatija-fullname energiatodistus)])
                         :description    (-> energiatodistus :perustiedot :rakennustunnus)
                         :attach         {:contact (asha/kayttaja->contact laatija)}}))))

(defn create-tietopyynto [sender-id request-id case-number users document]
  (asha/execute-operation {:sender-id         sender-id
                           :request-id        request-id
                           :identity          {:case              {:number case-number}
                                               :processing-action {:name-identity "Vireillepano"}}
                           :processing-action {:name                 "Tietopyyntö"
                                               :reception-date       (java.time.Instant/now)
                                               :expected-end-date    (.plus (java.time.Instant/now) 30 java.time.temporal.ChronoUnit/DAYS)
                                               :contacting-direction "SENT"
                                               :contact              (map asha/kayttaja->contact users)}})
  (asha/add-documents-to-processing-action sender-id request-id case-number "Tietopyyntö" [(assoc document :type "Pyyntö")])
  (asha/take-processing-action sender-id request-id case-number "Tietopyyntö"))
