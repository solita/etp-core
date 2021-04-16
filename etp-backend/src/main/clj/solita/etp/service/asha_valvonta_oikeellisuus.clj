(ns solita.etp.service.asha-valvonta-oikeellisuus
  (:require [solita.etp.service.asha :as asha]))

(defn create-case [sender-id request-id name description users]
  (asha/case-create {:request-id     request-id
                     :sender-id      sender-id
                     :classification "05.03.02"
                     :service        "general"              ; Yleinen menettely
                     :name           name
                     :description    description
                     :attach         {:contact (asha/kayttaja->contact users)}}))

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
