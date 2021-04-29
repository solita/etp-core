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
    (asha/open-case! {:request-id     (request-id energiatodistus id)
                      :sender-id      (:email whoami)
                      :classification "05.03.02"
                      :service        "general"             ; Yleinen menettely
                      :name           (str/join "; " [(-> energiatodistus :id)
                                                      (str (:etunimi laatija) " " (:sukunimi laatija))])
                      :description    (str/join "\r" [(-> energiatodistus :perustiedot :nimi)
                                                      (str (-> energiatodistus :perustiedot :katuosoite-fi) ", "
                                                           (-> energiatodistus :perustiedot :postinumero))
                                                      (-> energiatodistus :perustiedot :rakennustunnus)])
                      :attach         {:contact (asha/kayttaja->contact laatija)}})))

(defn- available-processing-actions [toimenpide laatija]
  {:rfi-request   {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                       :processing-action {:name-identity "Vireillepano"}}
                   :processing-action {:name                 "Tietopyyntö"
                                       :reception-date       (java.time.Instant/now)
                                       :contacting-direction "SENT"
                                       :contact              (asha/kayttaja->contact laatija)}
                   :document          {:type "Päätös" :name "tietopyyntö.txt"}}
   :rfi-order     {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                       :processing-action {:name-identity "Käsittely"}}
                   :processing-action {:name                 "Kehotuksen antaminen"
                                       :reception-date       (java.time.Instant/now)
                                       :contacting-direction "SENT"
                                       :contact              (asha/kayttaja->contact laatija)}
                   :document          {:type "Kirje" :name "kehotus_tietopyyntö.txt"}}
   :rfi-warning   {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                       :processing-action {:name-identity "Käsittely"}}
                   :processing-action {:name                 "Varoituksen antaminen"
                                       :reception-date       (java.time.Instant/now)
                                       :contacting-direction "SENT"
                                       :contact              (asha/kayttaja->contact laatija)}
                   :document          {:type "Kirje" :name "varoitus_tietopyyntö.txt"}}
   :audit-report  {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                       :processing-action {:name-identity "Käsittely"}}
                   :processing-action {:name                 "Valvontamuistion laatiminen"
                                       :reception-date       (java.time.Instant/now)
                                       :contacting-direction "SENT"
                                       :contact              (asha/kayttaja->contact laatija)}
                   :document          {:type "Muistio" :name "valvontamuistio.txt"}}
   :audit-order   {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                       :processing-action {:name-identity "Käsittely"}}
                   :processing-action {:name                 "Kehotuksen antaminen valvontamuistion perusteella"
                                       :reception-date       (java.time.Instant/now)
                                       :contacting-direction "SENT"
                                       :contact              (asha/kayttaja->contact laatija)}
                   :document          {:type "Muistio" :name "kehotus_valvontamuistio.txt"}}
   :audit-warning {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                       :processing-action {:name-identity "Käsittely"}}
                   :processing-action {:name                 "Varoituksen antaminen valvontamuistion perusteella"
                                       :reception-date       (java.time.Instant/now)
                                       :contacting-direction "SENT"
                                       :contact              (asha/kayttaja->contact laatija)}
                   :document          {:type "Muistio" :name "varoitus_valvontamuistio.txt"}}})

(defn log-toimenpide! [db whoami id toimenpide]
  (let [{:keys [energiatodistus laatija]} (resolve-energiatodistus-laatija db id)
        processing-action (get (available-processing-actions toimenpide laatija)
                               (toimenpide/type-key (:type-id toimenpide)))
        request-id (request-id energiatodistus id)
        sender-id (:email whoami)
        case-number (:diaarinumero toimenpide)
        ; TODO: get document from toimenpide
        document "Testi" #_(:document toimenpide)]

    (asha/move-processing-action!
      sender-id
      request-id
      case-number
      (-> processing-action :identity :processing-action :name-identity))
    (asha/take-processing-action!
      sender-id
      request-id
      case-number
      (-> processing-action :identity :processing-action :name-identity))

    (asha/execute-operation! {:request-id        request-id
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
    (asha/take-processing-action! sender-id request-id case-number (-> processing-action :processing-action :name))
    (asha/mark-processing-action-as-ready!
      sender-id
      request-id
      case-number
      (-> processing-action :processing-action :name))))

(defn close-case! [db whoami id toimenpide]
  (let [{:keys [energiatodistus]} (resolve-energiatodistus-laatija db (:energiatodistus-id toimenpide))]
    (asha/close-case! (:email whoami) (request-id energiatodistus id) (:diaarinumero toimenpide))))
