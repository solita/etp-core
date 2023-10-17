(ns solita.etp.service.valvonta-kaytto.asha
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [solita.common.time :as time]
            [solita.etp.exception :as exception]
            [solita.etp.service.asha :as asha]
            [solita.etp.service.valvonta-kaytto.hallinto-oikeus-attachment :as hao-attachment]
            [solita.etp.service.valvonta-kaytto.toimenpide :as toimenpide]
            [solita.etp.service.valvonta-kaytto.template :as template]
            [solita.etp.service.valvonta-kaytto.store :as store]
            [solita.etp.service.valvonta-kaytto.osapuoli :as osapuoli]
            [solita.etp.service.pdf :as pdf]
            [solita.etp.db :as db]
            [solita.common.formats :as formats])
  (:import (java.time Instant)))

(db/require-queries 'valvonta-kaytto)
(db/require-queries 'geo)
(db/require-queries 'hallinto-oikeus)
(db/require-queries 'karajaoikeus)

(defn toimenpide-type->document [type-id]
  (let [type-key (toimenpide/type-key type-id)
        documents {:rfi-request                     {:type "Pyyntö" :filename "tietopyynto.pdf"}
                   :rfi-order                       {:type "Kirje" :filename "kehotus.pdf"}
                   :rfi-warning                     {:type "Kirje" :filename "varoitus.pdf"}
                   :decision-order-hearing-letter   {:type     "Kirje"
                                                     :filename "kuulemiskirje.pdf"}
                   :decision-order-actual-decision  {:type     "Kirje"
                                                     :filename "kaskypaatos.pdf"}
                   :decision-order-notice-bailiff   {:type     "Kirje"
                                                     :filename "haastemies-tiedoksianto.pdf"}
                   :penalty-decision-hearing-letter {:type     "Kirje"
                                                     :filename "sakkopaatos-kuulemiskirje.pdf"}}]
    (get documents type-key)))

(defn find-kaytto-valvonta-documents [db valvonta-id]
  (->> (valvonta-kaytto-db/select-valvonta-documents db {:valvonta-id valvonta-id})
       (map (fn [toimenpide]
              (let [type (toimenpide/type-key (:type-id toimenpide))]
                {type (:publish-time toimenpide)})))
       (into {})))

(defn- find-ilmoituspaikka [ilmoituspaikat valvonta]
  (if (osapuoli/ilmoituspaikka-other? valvonta)
    (:ilmoituspaikka-description valvonta)
    (->> ilmoituspaikat (filter #(= (:id %) (:ilmoituspaikka-id valvonta))) first :label-fi)))

(defn- find-postitoimipaikka [db postinumero]
  (-> (geo-db/select-postinumero-by-id db {:id (formats/string->int postinumero)}) first :label-fi))

(defn- template-optional [value] (if (some? value) [value] []))

(defn- tiedoksi-saaja [roolit osapuoli]
  (let [rooli (first (filter #(= (:id %) (:rooli-id osapuoli)) roolit))]
    {:nimi  (or (:nimi osapuoli) (str (:etunimi osapuoli) " " (:sukunimi osapuoli)))
     :rooli (template-optional
              (if (osapuoli/other-rooli? osapuoli)
                (:rooli-description osapuoli)
                (when (some? rooli) (str (:label-fi rooli) "/" (:label-sv rooli)))))
     :email (template-optional (:email osapuoli))}))

(defn kuulemiskirje-data
  "Returns diaarinumero and fine amount of the newest
   kuulemiskirje-toimenpide associated with the given valvonta-id."
  [db valvonta-id]
  (->> {:valvonta-id valvonta-id}
       (valvonta-kaytto-db/kuulemiskirje-data db)
       first))

(defn past-dates-for-kaskypaatos-toimenpiteet
  "Retrieves the dates of kehotus, varoitus and kuulemiskirje toimenpiteet with
   the given valvonta-id and formats them for displaying in a document"
  [db valvonta-id]
  (let [data (->> {:valvonta-id valvonta-id}
                  (valvonta-kaytto-db/past-dates-for-kaskypaatos-toimenpiteet db)
                  first)]
    (update-vals data time/format-date)))

(defn hallinto-oikeus-id->formatted-strings [db hallinto-oikeus-id]
  (if-let [formatted-strings (first (hallinto-oikeus-db/find-document-template-wording-by-hallinto-oikeus-id db {:hallinto-oikeus-id hallinto-oikeus-id}))]
    formatted-strings
    (exception/throw-ex-info!
      {:message (str "Unknown hallinto-oikeus-id: " hallinto-oikeus-id)})))

(defn find-administrative-court-id-from-osapuoli-specific-data [osapuoli-specific-data osapuoli-id]
  (->> osapuoli-specific-data
       (filter #(= (:osapuoli-id %) osapuoli-id))
       first
       :hallinto-oikeus-id))

(defmulti format-type-specific-data
          (fn [_db toimenpide _osapuoli-id] (-> toimenpide :type-id toimenpide/type-key)))

(defmethod format-type-specific-data :decision-order-actual-decision [db toimenpide osapuoli-id]
  (let [recipient-answered? (-> toimenpide :type-specific-data :recipient-answered)
        hallinto-oikeus-strings (hallinto-oikeus-id->formatted-strings
                                  db
                                  (-> toimenpide
                                      :type-specific-data
                                      :osapuoli-specific-data
                                      (find-administrative-court-id-from-osapuoli-specific-data osapuoli-id)))]
    {:vastaus-fi               (str (if recipient-answered?
                                      "Asianosainen antoi vastineen kuulemiskirjeeseen."
                                      "Asianosainen ei vastannut kuulemiskirjeeseen.")
                                    " "
                                    (-> toimenpide :type-specific-data :answer-commentary-fi))
     :vastaus-sv               (str (if recipient-answered?
                                      "gav ett bemötande till brevet om hörande."
                                      "svarade inte på brevet om hörande.")
                                    " "
                                    (-> toimenpide :type-specific-data :answer-commentary-sv))
     :oikeus-fi                (:fi hallinto-oikeus-strings)
     :oikeus-sv                (:sv hallinto-oikeus-strings)
     :fine                     (-> toimenpide :type-specific-data :fine)
     :statement-fi             (-> toimenpide :type-specific-data :statement-fi)
     :statement-sv             (-> toimenpide :type-specific-data :statement-sv)
     :department-head-name     (-> toimenpide :type-specific-data :department-head-name)
     :department-head-title-fi (-> toimenpide :type-specific-data :department-head-title-fi)
     :department-head-title-sv (-> toimenpide :type-specific-data :department-head-title-sv)}))

(defn- karajaoikeus-id->name [db id]
  (first (karajaoikeus-db/find-karajaoikeus-name-by-id db {:karajaoikeus-id id})))

(defmethod format-type-specific-data :decision-order-notice-bailiff [db toimenpide osapuoli-id]
  (let [osapuoli (->> toimenpide
                      :type-specific-data
                      :osapuoli-specific-data
                      (filter #(= (:osapuoli-id %) osapuoli-id))
                      first)
        karajaoikeus-id (:karajaoikeus-id osapuoli)
        haastemies-email (:haastemies-email osapuoli)]
    {:karajaoikeus     (karajaoikeus-id->name db karajaoikeus-id)
     :haastemies-email haastemies-email}))

(defmethod format-type-specific-data :default [_ toimenpide _]
  (:type-specific-data toimenpide))

(defn- template-data [db whoami valvonta toimenpide osapuoli dokumentit ilmoituspaikat tiedoksi roolit]
  {:päivä                  (time/today)
   :määräpäivä             (time/format-date (:deadline-date toimenpide))
   :diaarinumero           (:diaarinumero toimenpide)
   :valvoja                (select-keys whoami [:etunimi :sukunimi :email :puhelin])
   :omistaja-henkilo       (when (osapuoli/henkilo? osapuoli)
                             (-> (select-keys osapuoli [:etunimi :sukunimi :jakeluosoite :postinumero :postitoimipaikka :henkilotunnus])
                                 (update-in [:henkilotunnus] (fn [henkilotunnus] (when henkilotunnus (string/replace henkilotunnus "-" "‑")))))) ; Replace hyphen with non-breaking variant
   :omistaja-yritys        (when (osapuoli/yritys? osapuoli)
                             (select-keys osapuoli [:nimi :jakeluosoite :postinumero :postitoimipaikka :vastaanottajan-tarkenne]))
   :kohde                  {:katuosoite       (:katuosoite valvonta)
                            :postinumero      (:postinumero valvonta)
                            :postitoimipaikka (find-postitoimipaikka db (:postinumero valvonta))
                            :ilmoituspaikka   (find-ilmoituspaikka ilmoituspaikat valvonta)
                            :ilmoitustunnus   (:ilmoitustunnus valvonta)
                            :havaintopäivä    (-> valvonta :havaintopaiva time/format-date)}
   :tietopyynto            {:tietopyynto-pvm         (time/format-date (:rfi-request dokumentit))
                            :tietopyynto-kehotus-pvm (time/format-date (:rfi-order dokumentit))}
   :tiedoksi               (map (partial tiedoksi-saaja roolit) tiedoksi)
   :tyyppikohtaiset-tiedot (format-type-specific-data db toimenpide (:id osapuoli))
   :aiemmat-toimenpiteet   (when (or (toimenpide/kaskypaatos-toimenpide? toimenpide)
                                     (toimenpide/sakkopaatos-toimenpide? toimenpide))
                             (merge
                               (kuulemiskirje-data db (:id valvonta))
                               (past-dates-for-kaskypaatos-toimenpiteet db (:id valvonta))))})

(defn- request-id [valvonta-id toimenpide-id]
  (str valvonta-id "/" toimenpide-id))

(defn- osapuoli->contact [osapuoli]
  (cond
    (osapuoli/henkilo? osapuoli)
    {:type          "PERSON"
     :first-name    (:etunimi osapuoli)
     :last-name     (:sukunimi osapuoli)
     :phone-number  (:puhelin osapuoli)
     :email-address (:email osapuoli)}
    (osapuoli/yritys? osapuoli)
    {:type                "ORGANIZATION"
     :organizational-name (:nimi osapuoli)
     :phone-number        (:puhelin osapuoli)
     :email-address       (:email osapuoli)}))

(defn- available-processing-actions [toimenpide osapuolet]
  {:rfi-request                           {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                                               :processing-action {:name-identity "Vireillepano"}}
                                           :processing-action {:name                 "Tietopyyntö"
                                                               :reception-date       (Instant/now)
                                                               :contacting-direction "SENT"
                                                               :contact              (map osapuoli->contact osapuolet)}
                                           :document          (toimenpide-type->document (:type-id toimenpide))}
   :rfi-order                             {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                                               :processing-action {:name-identity "Vireillepano"}}
                                           :processing-action {:name                 "Kehotuksen antaminen"
                                                               :reception-date       (Instant/now)
                                                               :contacting-direction "SENT"
                                                               :contact              (map osapuoli->contact osapuolet)}
                                           :document          (toimenpide-type->document (:type-id toimenpide))}
   :rfi-warning                           {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                                               :processing-action {:name-identity "Käsittely"}}
                                           :processing-action {:name                 "Varoituksen antaminen"
                                                               :reception-date       (Instant/now)
                                                               :contacting-direction "SENT"
                                                               :contact              (map osapuoli->contact osapuolet)}
                                           :document          (toimenpide-type->document (:type-id toimenpide))}
   :decision-order-hearing-letter         {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                                               :processing-action {:name-identity "Päätöksenteko"}}
                                           :document          (toimenpide-type->document (:type-id toimenpide))
                                           :processing-action {:name                 "Kuulemiskirje käskypäätöksestä"
                                                               :reception-date       (Instant/now)
                                                               :contacting-direction "SENT"
                                                               :contact              (map osapuoli->contact osapuolet)}}
   :decision-order-actual-decision        {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                                               :processing-action {:name-identity "Päätöksenteko"}}
                                           :document          (toimenpide-type->document (:type-id toimenpide))
                                           :processing-action {:name                 "Käskypäätös"
                                                               :reception-date       (Instant/now)
                                                               :contacting-direction "SENT"
                                                               :contact              (map osapuoli->contact osapuolet)}}
   :decision-order-notice-first-mailing   {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                                               :processing-action {:name-identity "Tiedoksianto ja toimeenpano"}}
                                           :document          (toimenpide-type->document (:type-id toimenpide))
                                           :processing-action {:name                 "Päätös tiedoksi - ensimmäinen postitus"
                                                               :reception-date       (Instant/now)
                                                               :contacting-direction "SENT"
                                                               :contact              (map osapuoli->contact osapuolet)}}
   :decision-order-notice-bailiff         {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                                               :processing-action {:name-identity "Tiedoksianto ja toimeenpano"}}
                                           :document          (toimenpide-type->document (:type-id toimenpide))
                                           :processing-action {:name                 "Asiakirjan toimituspyyntö haastemiehelle"
                                                               :reception-date       (Instant/now)
                                                               :contacting-direction "SENT"
                                                               :contact              (map osapuoli->contact osapuolet)}}
   :decision-order-waiting-for-deadline   {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                                               :processing-action {:name-identity "Valitusajan umpeutuminen"}}
                                           :document          (toimenpide-type->document (:type-id toimenpide))
                                           :processing-action {:name                 "Valitusajan umpeutuminen"
                                                               :reception-date       (Instant/now)
                                                               :contacting-direction "SENT"
                                                               :contact              (map osapuoli->contact osapuolet)}}
   :penalty-decision-hearing-letter       {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                                               :processing-action {:name-identity "Päätöksenteko"}}
                                           :document          (toimenpide-type->document (:type-id toimenpide))
                                           :processing-action {:name                 "Kuulemiskirje uhkasakkopäätöksestä"
                                                               :reception-date       (Instant/now)
                                                               :contacting-direction "SENT"
                                                               :contact              (map osapuoli->contact osapuolet)}}
   :penalty-decision-notice-first-mailing {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                                               :processing-action {:name-identity "Päätöksenteko"}}
                                           :document          (toimenpide-type->document (:type-id toimenpide))
                                           :processing-action {:name                 "Uhkasakkopäätös tiedoksi - ensimmäinen postitus"
                                                               :reception-date       (Instant/now)
                                                               :contacting-direction "SENT"
                                                               :contact              (map osapuoli->contact osapuolet)}}})

(defn- resolve-processing-action [toimenpide osapuolet]
  (let [processing-actions (available-processing-actions toimenpide osapuolet)
        type-key (toimenpide/type-key (:type-id toimenpide))]
    (get processing-actions type-key)))

(defn open-case! [db whoami valvonta osapuolet ilmoituspaikat]
  (asha/open-case! {:request-id     (request-id (:id valvonta) 1)
                    :sender-id      (:email whoami)
                    :classification "05.03.01"
                    :service        "general"               ; Yleinen menettely
                    :name           (asha/string-join ", " [(:rakennustunnus valvonta)
                                                            (:katuosoite valvonta)
                                                            (asha/string-join " " [(:postinumero valvonta)
                                                                                   (find-postitoimipaikka db (:postinumero valvonta))])])

                    :description    (asha/string-join "\r" [(find-ilmoituspaikka ilmoituspaikat valvonta)
                                                            (:ilmoitustunnus valvonta)])
                    :attach         {:contact (map osapuoli->contact osapuolet)}}))

(defn add-hallinto-oikeus-attachment
  "Adds hallinto-oikeus specific attachment to the end of the given pdf"
  [db pdf hallinto-oikeus-id]
  (pdf/merge-pdf [(io/input-stream pdf)
                  (hao-attachment/attachment-for-hallinto-oikeus-id db hallinto-oikeus-id)]))

(defn generate-pdf-document
  [db whoami valvonta toimenpide ilmoituspaikat osapuoli osapuolet roolit]
  (let [template-id (:template-id toimenpide)
        template (-> (valvonta-kaytto-db/select-template db {:id template-id}) first)
        documents (find-kaytto-valvonta-documents db (:id valvonta))
        tiedoksi (if (template/send-tiedoksi? template) (filter osapuoli/tiedoksi? osapuolet) [])
        template-data (template-data db whoami valvonta toimenpide
                                     osapuoli documents ilmoituspaikat
                                     tiedoksi roolit)
        generated-pdf (pdf/generate-pdf->bytes {:template (:content template)
                                                :data     template-data})]
    (if (toimenpide/kaskypaatos-varsinainen-paatos? toimenpide)
      (add-hallinto-oikeus-attachment db generated-pdf (-> toimenpide
                                                           :type-specific-data
                                                           :osapuoli-specific-data
                                                           (find-administrative-court-id-from-osapuoli-specific-data (:id osapuoli))))
      generated-pdf)))

(defn filter-osapuolet-with-no-document
  "If toimenpidetype of the toimenpide is such that the document might not be created for some,
  osapuolet will be filtered so that only those are returned that have a :document as true
  specified in type-specific-data of the toimenpide.
  For all other toimenpidetypes all osapuolet are returned."
  [toimenpide osapuolet]
  (if ((some-fn toimenpide/kaskypaatos-varsinainen-paatos? toimenpide/kaskypaatos-haastemies-tiedoksianto?) toimenpide)
    (let [osapuolet-with-document (->> toimenpide
                                       :type-specific-data
                                       :osapuoli-specific-data
                                       (filter #(true? (:document %)))
                                       (map :osapuoli-id)
                                       set)]
      (filter #(contains? osapuolet-with-document (:id %)) osapuolet))
    osapuolet))

(defn log-toimenpide! [db aws-s3-client whoami valvonta toimenpide osapuolet ilmoituspaikat roolit]
  (let [request-id (request-id (:id valvonta) (:id toimenpide))
        sender-id (:email whoami)
        case-number (:diaarinumero toimenpide)
        processing-action (resolve-processing-action toimenpide osapuolet)
        documents (when (:document processing-action)
                    (->> osapuolet
                         (filter osapuoli/omistaja?)
                         (filter-osapuolet-with-no-document toimenpide)
                         (map (fn [osapuoli]
                                (let [document (generate-pdf-document db whoami valvonta toimenpide ilmoituspaikat
                                                                      osapuoli osapuolet roolit)]
                                  (store/store-document aws-s3-client (:id valvonta) (:id toimenpide) osapuoli document)
                                  document)))))]
    (asha/log-toimenpide!
      sender-id
      request-id
      case-number
      processing-action
      documents)))

(defn close-case! [whoami valvonta-id toimenpide]
  (asha/close-case!
    (:email whoami)
    (request-id valvonta-id (:id toimenpide))
    (:diaarinumero toimenpide)
    (:description toimenpide)))
