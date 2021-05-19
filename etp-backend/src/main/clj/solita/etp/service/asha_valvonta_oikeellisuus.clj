(ns solita.etp.service.asha-valvonta-oikeellisuus
  (:require [solita.etp.service.asha :as asha]
            [solita.etp.service.complete-energiatodistus :as complete-energiatodistus-service]
            [solita.etp.service.kayttaja :as kayttaja-service]
            [clojure.string :as str]
            [solita.etp.exception :as exception]
            [solita.etp.service.toimenpide :as toimenpide]
            [solita.etp.service.pdf :as pdf]))

(defn- resolve-energiatodistus-laatija [db energiatodistus-id]
  (let [energiatodistus (complete-energiatodistus-service/find-complete-energiatodistus db energiatodistus-id)
        laatija (kayttaja-service/find-kayttaja db (:laatija-id energiatodistus))]
    (if (and energiatodistus laatija)
      {:energiatodistus energiatodistus
       :laatija         laatija}
      (exception/throw-ex-info!
        :failed-to-resolve-energiatodistus-or-laatija-from-toimenpide
        "Failed to resolve energiatodistus or laatija from toimenpide"))))

(defn- request-id [energiatodistus id]
  (str (:id energiatodistus) "/" id))

(defn- available-processing-actions [toimenpide laatija]
  {:rfi-request   {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                       :processing-action {:name-identity "Vireillepano"}}
                   :processing-action {:name                 "Tietopyyntö"
                                       :reception-date       (java.time.Instant/now)
                                       :contacting-direction "SENT"
                                       :contact              (asha/kayttaja->contact laatija)}
                   :document          {:type "Pyyntö" :name "tietopyyntö.txt"}}
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
                   :document          {:type "Kirje" :name "kehotus_valvontamuistio.txt"}}
   :audit-warning {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                       :processing-action {:name-identity "Käsittely"}}
                   :processing-action {:name                 "Varoituksen antaminen valvontamuistion perusteella"
                                       :reception-date       (java.time.Instant/now)
                                       :contacting-direction "SENT"
                                       :contact              (asha/kayttaja->contact laatija)}
                   :document          {:type "Kirje" :name "varoitus_valvontamuistio.txt"}}
   :rfc-request   {:identity          {:case {:number (:diaarinumero toimenpide)}}
                   :processing-action {:name           "Lisäselvityspyyntö"
                                       :reception-date (java.time.Instant/now)
                                       :description    "Lähetetty lisäselvityspyyntö on tallennettu energiatodistusrekisteriin"}}})

(defn- resolve-processing-action [sender-id request-id case-number toimenpide laatija]
  (let [processing-actions (available-processing-actions toimenpide laatija)
        type-key (toimenpide/type-key (:type-id toimenpide))
        update-latest-processsing-action (fn [processing-action]
                                           (assoc processing-action
                                             :processing-action
                                             {:name-identity (asha/resolve-latest-case-processing-action-state
                                                               sender-id
                                                               request-id
                                                               case-number)}))]
    (cond-> (get processing-actions type-key)
            (= type-key :rfc-request) (update :identity update-latest-processsing-action))))

(defn- string-join [separator coll]
  (str/join separator (->> coll
                           (map str)
                           (remove empty?))))

(defn open-case! [db whoami id]
  (let [{:keys [energiatodistus laatija]} (resolve-energiatodistus-laatija db id)]
    (asha/open-case! {:request-id     (request-id energiatodistus id)
                      :sender-id      (:email whoami)
                      :classification "05.03.02"
                      :service        "general"             ; Yleinen menettely
                      :name           (string-join "; " [(-> energiatodistus :id)
                                                         (string-join " " [(:etunimi laatija)
                                                                           (:sukunimi laatija)])])
                      :description    (string-join "\r" [(-> energiatodistus :perustiedot :nimi)
                                                         (string-join ", " [(-> energiatodistus :perustiedot :katuosoite-fi)
                                                                            (string-join " " [(-> energiatodistus :perustiedot :postinumero)
                                                                                              (-> energiatodistus :perustiedot :postitoimipaikka-fi)])])
                                                         (-> energiatodistus :perustiedot :rakennustunnus)])
                      :attach         {:contact (asha/kayttaja->contact laatija)}})))

(defn log-toimenpide! [db whoami id toimenpide]
  (let [{:keys [energiatodistus laatija]} (resolve-energiatodistus-laatija db id)
        request-id (request-id energiatodistus id)
        sender-id (:email whoami)
        case-number (:diaarinumero toimenpide)
        processing-action (resolve-processing-action sender-id request-id case-number toimenpide laatija)
        document (with-open [baos (java.io.ByteArrayOutputStream.)]
                   (pdf/html->pdf "base" {} baos)
                   (.toByteArray baos))]
    (asha/log-toimenpide!
      sender-id
      request-id
      case-number
      processing-action
      document)))

(defn close-case! [db whoami id toimenpide]
  (let [{:keys [energiatodistus]} (resolve-energiatodistus-laatija db (:energiatodistus-id toimenpide))]
    (asha/close-case!
      (:email whoami)
      (request-id energiatodistus id)
      (:diaarinumero toimenpide)
      (:description toimenpide))))