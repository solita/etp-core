(ns solita.etp.service.asha-valvonta-oikeellisuus
  (:require [solita.etp.service.asha :as asha]
            [solita.etp.service.energiatodistus :as energiatodistus-service]
            [solita.etp.service.kayttaja :as kayttaja-service]
            [clojure.string :as str]
            [solita.etp.exception :as exception]
            [solita.etp.service.toimenpide :as toimenpide]))

(defn resolve-energiatodistus-laatija [db energiatodistus-id]
  (let [energiatodistus (energiatodistus-service/find-energiatodistus-any-laatija db energiatodistus-id)
        laatija (kayttaja-service/find-kayttaja db (:laatija-id energiatodistus))]
    (if (and energiatodistus laatija)
      {:energiatodistus energiatodistus
       :laatija         laatija}
      (exception/throw-ex-info!
        :failed-to-resolve-energiatodistus-or-laatija-from-toimenpide
        "Failed to resolve energiatodistus or laatija from toimenpide"))))

(defn request-id [energiatodistus id]
  (str (:id energiatodistus) "/" id))

(defn open-case! [db whoami id]
  (let [{:keys [energiatodistus laatija]} (resolve-energiatodistus-laatija db id)]
    (asha/open-case! {:request-id    (request-id energiatodistus id)
                     :sender-id      (:email whoami)
                     :classification "05.03.02"
                     :service        "general"              ; Yleinen menettely
                     :name           (str/join ", " [(-> energiatodistus :perustiedot :katuosoite-fi)
                                                     (-> energiatodistus :perustiedot :postinumero)
                                                     (:laatija-fullname energiatodistus)])
                     :description    (str "Rakennustunnus: " (-> energiatodistus :perustiedot :rakennustunnus))
                     :attach         {:contact (asha/kayttaja->contact laatija)}})))

(defn- available-processing-actions [toimenpide laatija]
  {:rfi-request {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                     :processing-action {:name-identity "Vireillepano"}}
                 :processing-action {:name                 "Tietopyyntö"
                                     :reception-date       (java.time.Instant/now)
                                     :expected-end-date    (or (:deadline-date toimenpide)
                                                               (.plus (java.time.Instant/now) 30 java.time.temporal.ChronoUnit/DAYS))
                                     :contacting-direction "SENT"
                                     :contact              (asha/kayttaja->contact laatija)}
                 :document          {:type "Päätös" :name "Tietopyyntö.txt"}}})

(defn log-toimenpide! [db whoami id toimenpide]
  (let [{:keys [energiatodistus laatija]} (resolve-energiatodistus-laatija db id)
        processing-action (get (available-processing-actions toimenpide laatija) (toimenpide/type-key (:type-id toimenpide)))
        request-id (request-id energiatodistus id)
        sender-id (:email whoami)
        case-number (:diaarinumero toimenpide)
        document (:document toimenpide)]
    (asha/execute-operation! {:request-id       request-id
                             :sender-id         sender-id
                             :identity          (:identity processing-action)
                             :processing-action (:processing-action processing-action)})
    (when document
      (asha/add-documents-to-processing-action!
        sender-id
        request-id
        case-number
        (-> processing-action :processing-action :name)
        [{:content (.getBytes document)
          :type    (-> processing-action :document :type)
          :name    (-> processing-action :document :name)}]))
    (asha/take-processing-action! sender-id request-id case-number (-> processing-action :processing-action :name))))

(defn close-case! [db whoami id toimenpide]
  (let [{:keys [energiatodistus]} (resolve-energiatodistus-laatija db (:energiatodistus-id toimenpide))]
    (asha/close-case! (:email whoami) (request-id energiatodistus id) (:diaarinumero toimenpide))))