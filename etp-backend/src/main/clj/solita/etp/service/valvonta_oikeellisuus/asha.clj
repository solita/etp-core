(ns solita.etp.service.valvonta-oikeellisuus.asha
  (:require [solita.common.time :as time]
            [solita.common.maybe :as maybe]
            [solita.etp.service.asha :as asha]
            [solita.etp.service.complete-energiatodistus :as complete-energiatodistus-service]
            [solita.etp.service.kayttaja :as kayttaja-service]
            [solita.etp.service.valvonta-oikeellisuus.toimenpide :as toimenpide]
            [solita.etp.service.pdf :as pdf]
            [solita.etp.db :as db]
            [solita.etp.service.file :as file-service]
            [solita.etp.exception :as exception]))

(db/require-queries 'valvonta-oikeellisuus)

(def file-key-prefix "valvonta/oikeellisuus")

(defn- file-path [energiatodistus-id toimenpide-id]
  (str file-key-prefix "/" energiatodistus-id "/" toimenpide-id))

(defn toimenpide-type->document [type-id]
  (let [type-key (toimenpide/type-key type-id )
        documents {:rfi-request {:type "Pyyntö" :filename "tietopyynto.pdf"}
                   :rfi-order {:type "Kirje" :filename "tietopyynto_kehotus.pdf"}
                   :rfi-warning {:type "Kirje" :filename "tietopyynto_varoitus.pdf"}
                   :audit-report {:type "Muistio" :filename "valvontamuistio.pdf"}
                   :audit-order {:type "Kirje" :filename "valvontamuistio_kehotus.pdf"}
                   :audit-warning  {:type "Kirje" :filename "valvontamuistio_varoitus.pdf"}}]
    (get documents type-key)))

(defn- store-document [aws-s3-client energiatodistus-id toimenpide-id document]
  (file-service/upsert-file-from-bytes
    aws-s3-client
    (file-path energiatodistus-id toimenpide-id)
    document))

(defn find-document [aws-s3-client energiatodistus-id toimenpide-id]
  (file-service/find-file aws-s3-client (file-path energiatodistus-id toimenpide-id)))

(defn find-energiatodistus-valvonta-documents [db id]
  (->> (valvonta-oikeellisuus-db/select-energiatodistus-valvonta-documents db {:energiatodistus-id id})
       (map (fn [toimenpide]
              (let [type (toimenpide/type-key (:type-id toimenpide))]
                {type (:publish-time toimenpide)})))
       (into {})))

(defn- template-data [whoami toimenpide laatija energiatodistus dokumentit]
  {:päivä           (time/today)
   :määräpäivä      (time/format-date (:deadline-date toimenpide))
   :diaarinumero    (:diaarinumero toimenpide)
   :valvoja         (select-keys whoami [:etunimi :sukunimi :email])
   :laatija         (select-keys laatija [:etunimi :sukunimi :henkilotunnus :email :puhelin])
   :energiatodistus {:tunnus              (:id energiatodistus)
                     :rakennustunnus      (-> energiatodistus :perustiedot :rakennustunnus)
                     :nimi                (-> energiatodistus :perustiedot :nimi)
                     :katuosoite-fi       (-> energiatodistus :perustiedot :katuosoite-fi)
                     :katuosoite-sv       (-> energiatodistus :perustiedot :katuosoite-sv)
                     :postinumero         (-> energiatodistus :perustiedot :postinumero)
                     :postitoimipaikka-fi (-> energiatodistus :perustiedot :postitoimipaikka-fi)
                     :postitoimipaikka-sv (-> energiatodistus :perustiedot :postitoimipaikka-sv)}
   :tietopyynto     {:tietopyynto-pvm             (time/format-date (:rfi-request dokumentit))
                     :tietopyynto-kehotus-pvm     (time/format-date (:rfi-order dokumentit))}
   :valvontamuistio {:valvontamuistio-pvm         (time/format-date (:audit-report dokumentit))
                     :valvontamuistio-kehotus-pvm (time/format-date (:audit-order dokumentit))
                     :virheet                     (:virheet toimenpide)
                     :vakavuus                    (when-let [luokka (:severity-id toimenpide)]
                                                    (case luokka
                                                      0 {:ei-huomioitavaa true}
                                                      1 {:ei-toimenpiteitä true}
                                                      2 {:virheellinen true}))}})

(defn- find-resources [db energiatodistus-id]
  (when-let [energiatodistus (complete-energiatodistus-service/find-complete-energiatodistus db energiatodistus-id)]
    {:energiatodistus energiatodistus
     :laatija         (kayttaja-service/find-kayttaja db (:laatija-id energiatodistus))}))

(defn- get-resources! [db energiatodistus-id]
  (maybe/require-some! (str "Energiatodistus " energiatodistus-id " does not exist.")
                       (find-resources db energiatodistus-id)))

(defn generate-pdf-document
  ([db whoami toimenpide energiatodistus-id]
   (when-let [{:keys [energiatodistus laatija]} (find-resources db energiatodistus-id)]
     (generate-pdf-document db whoami toimenpide energiatodistus laatija)))
  ([db whoami toimenpide energiatodistus laatija]
    (let [template-id (:template-id toimenpide)
          template (->> (valvonta-oikeellisuus-db/select-template db {:id template-id})
                        first :content
                        (exception/require-some! :template template-id))
          documents (find-energiatodistus-valvonta-documents db (:id energiatodistus))
          template-data (template-data whoami toimenpide laatija energiatodistus documents)]
      (pdf/generate-pdf->bytes template template-data))))

(defn- request-id [energiatodistus-id toimenpide-id]
  (str energiatodistus-id "/" toimenpide-id))

(defn- kayttaja->contact [kayttaja]
  {:type          "ORGANIZATION"                            ;No enum constant fi.ys.eservice.entity.ContactType.PERSON
   :first-name    (:etunimi kayttaja)
   :last-name     (:sukunimi kayttaja)
   :email-address (:email kayttaja)})

(defn- available-processing-actions [toimenpide laatija]
  {:rfi-request   {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                       :processing-action {:name-identity "Vireillepano"}}
                   :processing-action {:name                 "Tietopyyntö"
                                       :reception-date       (java.time.Instant/now)
                                       :contacting-direction "SENT"
                                       :contact              (kayttaja->contact laatija)}
                   :document          (toimenpide-type->document (:type-id toimenpide))}
   :rfi-order     {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                       :processing-action {:name-identity "Käsittely"}}
                   :processing-action {:name                 "Kehotuksen antaminen"
                                       :reception-date       (java.time.Instant/now)
                                       :contacting-direction "SENT"
                                       :contact              (kayttaja->contact laatija)}
                   :document          (toimenpide-type->document (:type-id toimenpide))}
   :rfi-warning   {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                       :processing-action {:name-identity "Käsittely"}}
                   :processing-action {:name                 "Varoituksen antaminen"
                                       :reception-date       (java.time.Instant/now)
                                       :contacting-direction "SENT"
                                       :contact              (kayttaja->contact laatija)}
                   :document          (toimenpide-type->document (:type-id toimenpide))}
   :audit-report  {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                       :processing-action {:name-identity "Käsittely"}}
                   :processing-action {:name                 "Valvontamuistion laatiminen"
                                       :reception-date       (java.time.Instant/now)
                                       :contacting-direction "SENT"
                                       :contact              (kayttaja->contact laatija)}
                   :document          (toimenpide-type->document (:type-id toimenpide))}
   :audit-order   {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                       :processing-action {:name-identity "Käsittely"}}
                   :processing-action {:name                 "Kehotuksen antaminen valvontamuistion perusteella"
                                       :reception-date       (java.time.Instant/now)
                                       :contacting-direction "SENT"
                                       :contact              (kayttaja->contact laatija)}
                   :document          (toimenpide-type->document (:type-id toimenpide))}
   :audit-warning {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                       :processing-action {:name-identity "Käsittely"}}
                   :processing-action {:name                 "Varoituksen antaminen valvontamuistion perusteella"
                                       :reception-date       (java.time.Instant/now)
                                       :contacting-direction "SENT"
                                       :contact              (kayttaja->contact laatija)}
                   :document          (toimenpide-type->document (:type-id toimenpide))}
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

(defn open-case! [db whoami energiatodistus-id]
  (let [{:keys [energiatodistus laatija]} (get-resources! db energiatodistus-id)]
    (asha/open-case! {:request-id     (request-id energiatodistus-id 1)
                      :sender-id      (:email whoami)
                      :classification "05.03.02"
                      :service        "general"             ; Yleinen menettely
                      :name           (asha/string-join "; " [(-> energiatodistus :id)
                                                              (asha/string-join " " [(:etunimi laatija)
                                                                                     (:sukunimi laatija)])])
                      :description    (asha/string-join "\r" [(-> energiatodistus :perustiedot :nimi)
                                                              (asha/string-join ", " [(-> energiatodistus :perustiedot :katuosoite-fi)
                                                                                      (asha/string-join " " [(-> energiatodistus :perustiedot :postinumero)
                                                                                                             (-> energiatodistus :perustiedot :postitoimipaikka-fi)])])
                                                              (-> energiatodistus :perustiedot :rakennustunnus)])
                      :attach         {:contact (kayttaja->contact laatija)}})))

(defn log-toimenpide! [db aws-s3-client whoami energiatodistus-id toimenpide]
  (let [{:keys [energiatodistus laatija]} (get-resources! db energiatodistus-id)
        request-id (request-id energiatodistus-id (:id toimenpide))
        sender-id (:email whoami)
        case-number (:diaarinumero toimenpide)
        processing-action (resolve-processing-action sender-id request-id case-number toimenpide laatija)
        documents (when (:document processing-action)
                    (let [document (generate-pdf-document db whoami toimenpide energiatodistus laatija)]
                      (store-document aws-s3-client energiatodistus-id (:id toimenpide) document)
                      [document]))]
    (asha/log-toimenpide!
      sender-id
      request-id
      case-number
      processing-action
      documents)))

(defn close-case! [whoami energiatodistus-id toimenpide]
  (asha/close-case!
    (:email whoami)
    (request-id energiatodistus-id (:id toimenpide))
    (:diaarinumero toimenpide)
    (:description toimenpide)))