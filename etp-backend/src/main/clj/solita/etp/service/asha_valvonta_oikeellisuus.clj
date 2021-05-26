(ns solita.etp.service.asha-valvonta-oikeellisuus
  (:require [solita.common.time :as time]
            [solita.etp.service.asha :as asha]
            [solita.etp.service.complete-energiatodistus :as complete-energiatodistus-service]
            [solita.etp.service.kayttaja :as kayttaja-service]
            [clojure.string :as str]
            [solita.etp.exception :as exception]
            [solita.etp.service.toimenpide :as toimenpide]
            [solita.etp.service.pdf :as pdf]
            [clojure.java.io :as io]
            [solita.etp.service.file :as file-service]))

(def file-key-prefix "valvonta/oikeellisuus")

(defn- file-path [energiatodistus-id toimenpide-id]
  (str file-key-prefix "/" energiatodistus-id "/" toimenpide-id))

(defn store-document [aws-s3-client energiatodistus-id toimenpide-id document]
  (file-service/upsert-file-from-bytes aws-s3-client (file-path energiatodistus-id toimenpide-id) nil document))

(defn find-document [aws-s3-client energiatodistus-id toimenpide-id]
  (:content (file-service/find-file aws-s3-client (file-path energiatodistus-id toimenpide-id))))

(defn- template-data [whoami toimenpide laatija energiatodistus]
  (cond-> {:päivä           (time/today)
           :asha            {:diaarinumero (:diaarinumero toimenpide)}
           :valvoja         (select-keys whoami [:etunimi :sukunimi :email])
           :laatija         (select-keys laatija [:etunimi :sukunimi :henkilotunnus :email :puhelin])
           :energiatodistus {:tunnus              (str "ET-" (:id energiatodistus))
                             :rakennustunnus      (-> energiatodistus :perustiedot :rakennustunnus)
                             :nimi                (-> energiatodistus :perustiedot :nimi)
                             :katuosoite-fi       (-> energiatodistus :perustiedot :katuosoite-fi)
                             :katuosoite-sv       (-> energiatodistus :perustiedot :katuosoite-sv)
                             :postinumero         (-> energiatodistus :perustiedot :postinumero)
                             :postitoimipaikka-fi (-> energiatodistus :perustiedot :postitoimipaikka-fi)
                             :postitoimipaikka-sv (-> energiatodistus :perustiedot :postitoimipaikka-sv)}}))

(defn resolve-energiatodistus-laatija [db toimenpide]
  (when-let [energiatodistus-id (:energiatodistus-id toimenpide)]
    (let [energiatodistus (complete-energiatodistus-service/find-complete-energiatodistus db energiatodistus-id)
          laatija (kayttaja-service/find-kayttaja db (:laatija-id energiatodistus))]
      (if (and energiatodistus laatija)
        {:energiatodistus energiatodistus
         :laatija         laatija}
        (exception/throw-ex-info!
          :failed-to-resolve-energiatodistus-or-laatija-from-toimenpide
          "Failed to resolve energiatodistus or laatija from toimenpide")))))

(defn generate-template [whoami toimepide energiatodistus laatija]
  (let [template (slurp (io/resource "pdf/content-rfi-request-fi.html")) #_(:content toimepide)
        template-data (template-data whoami toimepide laatija energiatodistus)]
    {:template      template
     :template-data template-data}))

(defn- request-id [energiatodistus-id toimenpide-id]
  (str energiatodistus-id "/" toimenpide-id))

(defn- available-processing-actions [toimenpide laatija]
  {:rfi-request   {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                       :processing-action {:name-identity "Vireillepano"}}
                   :processing-action {:name                 "Tietopyyntö"
                                       :reception-date       (java.time.Instant/now)
                                       :contacting-direction "SENT"
                                       :contact              (asha/kayttaja->contact laatija)}
                   :document          {:type "Pyyntö" :name "tietopyyntö.pdf"}}
   :rfi-order     {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                       :processing-action {:name-identity "Käsittely"}}
                   :processing-action {:name                 "Kehotuksen antaminen"
                                       :reception-date       (java.time.Instant/now)
                                       :contacting-direction "SENT"
                                       :contact              (asha/kayttaja->contact laatija)}
                   :document          {:type "Kirje" :name "kehotus_tietopyyntö.pdf"}}
   :rfi-warning   {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                       :processing-action {:name-identity "Käsittely"}}
                   :processing-action {:name                 "Varoituksen antaminen"
                                       :reception-date       (java.time.Instant/now)
                                       :contacting-direction "SENT"
                                       :contact              (asha/kayttaja->contact laatija)}
                   :document          {:type "Kirje" :name "varoitus_tietopyyntö.pdf"}}
   :audit-report  {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                       :processing-action {:name-identity "Käsittely"}}
                   :processing-action {:name                 "Valvontamuistion laatiminen"
                                       :reception-date       (java.time.Instant/now)
                                       :contacting-direction "SENT"
                                       :contact              (asha/kayttaja->contact laatija)}
                   :document          {:type "Muistio" :name "valvontamuistio.pdf"}}
   :audit-order   {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                       :processing-action {:name-identity "Käsittely"}}
                   :processing-action {:name                 "Kehotuksen antaminen valvontamuistion perusteella"
                                       :reception-date       (java.time.Instant/now)
                                       :contacting-direction "SENT"
                                       :contact              (asha/kayttaja->contact laatija)}
                   :document          {:type "Kirje" :name "kehotus_valvontamuistio.pdf"}}
   :audit-warning {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                       :processing-action {:name-identity "Käsittely"}}
                   :processing-action {:name                 "Varoituksen antaminen valvontamuistion perusteella"
                                       :reception-date       (java.time.Instant/now)
                                       :contacting-direction "SENT"
                                       :contact              (asha/kayttaja->contact laatija)}
                   :document          {:type "Kirje" :name "varoitus_valvontamuistio.pdf"}}
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
  (let [{:keys [energiatodistus laatija]} (resolve-energiatodistus-laatija db {:energiatodistus-id id})]
    (asha/open-case! {:request-id     (request-id id 1)
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

(defn log-toimenpide! [db aws-s3-client whoami id toimenpide]
  (let [{:keys [energiatodistus laatija]} (resolve-energiatodistus-laatija db toimenpide)
        request-id (request-id id (:id toimenpide))
        sender-id (:email whoami)
        case-number (:diaarinumero toimenpide)
        processing-action (resolve-processing-action sender-id request-id case-number toimenpide laatija)
        document (when (:document processing-action)
                   (let [{:keys [template template-data]} (generate-template whoami toimenpide energiatodistus laatija)
                         bytes (pdf/generate-pdf->bytes template template-data)]
                     (store-document aws-s3-client (:energiatodistus-id toimenpide) (:id toimenpide) bytes)
                     bytes))]
    (asha/log-toimenpide!
      sender-id
      request-id
      case-number
      processing-action
      document)))

(defn close-case! [whoami id toimenpide]
  (asha/close-case!
    (:email whoami)
    (request-id id (:id toimenpide))
    (:diaarinumero toimenpide)
    (:description toimenpide)))