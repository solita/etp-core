(ns solita.etp.service.valvonta-kaytto.asha
  (:require [solita.common.time :as time]
            [solita.etp.service.asha :as asha]
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

(defn toimenpide-type->document [type-id]
  (let [type-key (toimenpide/type-key type-id )
        documents {:rfi-request {:type "Pyyntö" :filename "tietopyynto.pdf"}
                   :rfi-order {:type "Kirje" :filename "kehotus.pdf"}
                   :rfi-warning {:type "Kirje" :filename "varoitus.pdf"}
                   :decision-order-hearing-letter {:type "Kirje"
                                                   :filename "kuulemiskirje.pdf"}
                   :decision-order-actual-decision {:type "Kirje"
                                                    :filename "varsinainen-paatos.pdf"}}]
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
  (-> (geo-db/select-postinumero-by-id db {:id (formats/string->int postinumero)}) first :label-fi ))

(defn- template-optional [value] (if (some? value) [value] []))

(defn- tiedoksi-saaja [roolit osapuoli]
  (let [rooli (first (filter #(= (:id %) (:rooli-id osapuoli)) roolit))]
    {:nimi  (or (:nimi osapuoli) (str (:etunimi osapuoli) " " (:sukunimi osapuoli)))
     :rooli (template-optional
              (if (osapuoli/other-rooli? osapuoli)
                (:rooli-description osapuoli)
                (when (some? rooli) (str (:label-fi rooli) "/" (:label-sv rooli)))))
     :email (template-optional (:email osapuoli))}))

(defn past-dates-for-kaskypaatos-toimenpiteet
  "Retrieves the dates of kehotus, varoitus and kuulemiskirje toimenpiteet with
   the given valvonta-id and formats them for displaying in a document"
  [db valvonta-id]
  ;; TODO: Kun päivitetään Clojure 1.11:een, voidaan käyttää vain update-vals-funktiota
  (let [data (->> {:valvonta-id valvonta-id}
                  (valvonta-kaytto-db/past-dates-for-kaskypaatos-toimenpiteet db)
                  first)]
    (zipmap (keys data) (map time/format-date (vals data)))))

(defn hallinto-oikeus-id->formatted-string [hallinto-oikeus-id]
  (condp = hallinto-oikeus-id
    0 "Helsingin hallinto-oikeudelta"
    1 "Hämeenlinnan hallinto-oikeudelta"
    2 "Itä-Suomen hallinto-oikeudelta"
    3 "Pohjois-Suomen hallinto-oikeudelta"
    4 "Turun hallinto-oikeudelta"
    5 "Vaasan hallinto-oikeudelta"))

(defmulti format-type-specific-data
          (fn [toimenpide] (-> toimenpide :type-id toimenpide/type-key)))

(defmethod format-type-specific-data :decision-order-actual-decision [toimenpide]
  (let [recipient-answered? (-> toimenpide :type-specific-data :recipient-answered)
        answer-commentary (-> toimenpide :type-specific-data :answer-commentary)]
    (-> toimenpide
        :type-specific-data
        (assoc :vastaus
               (str (if recipient-answered?
                      "Asianosainen antoi vastineen kuulemiskirjeeseen."
                      "Asianosainen ei vastannut kuulemiskirjeeseen.")
                    " "
                    answer-commentary))
        (update :court hallinto-oikeus-id->formatted-string))))

(defmethod format-type-specific-data :default [toimenpide]
  (:type-specific-data toimenpide))

(defn- template-data [db whoami valvonta toimenpide osapuoli dokumentit ilmoituspaikat tiedoksi roolit]
  {:päivä            (time/today)
   :määräpäivä       (time/format-date (:deadline-date toimenpide))
   :diaarinumero     (:diaarinumero toimenpide)
   :valvoja          (select-keys whoami [:etunimi :sukunimi :email :puhelin])
   :omistaja-henkilo (when (osapuoli/henkilo? osapuoli)
                       (select-keys osapuoli [:etunimi :sukunimi :jakeluosoite :postinumero :postitoimipaikka]))
   :omistaja-yritys  (when (osapuoli/yritys? osapuoli)
                       (select-keys osapuoli [:nimi :jakeluosoite :postinumero :postitoimipaikka :vastaanottajan-tarkenne]))
   :kohde            {:katuosoite       (:katuosoite valvonta)
                      :postinumero      (:postinumero valvonta)
                      :postitoimipaikka (find-postitoimipaikka db (:postinumero valvonta))
                      :ilmoituspaikka   (find-ilmoituspaikka ilmoituspaikat valvonta)
                      :ilmoitustunnus   (:ilmoitustunnus valvonta)
                      :havaintopäivä    (-> valvonta :havaintopaiva time/format-date)}
   :tietopyynto      {:tietopyynto-pvm         (time/format-date (:rfi-request dokumentit))
                      :tietopyynto-kehotus-pvm (time/format-date (:rfi-order dokumentit))}
   :tiedoksi         (map (partial tiedoksi-saaja roolit) tiedoksi)
   :tyyppikohtaiset-tiedot (format-type-specific-data toimenpide)
   :aiemmat-toimenpiteet (when (toimenpide/kaskypaatos-toimenpide? toimenpide)
                           (past-dates-for-kaskypaatos-toimenpiteet db (:id valvonta)))})

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
  {:rfi-request {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                     :processing-action {:name-identity "Vireillepano"}}
                 :processing-action {:name                 "Tietopyyntö"
                                     :reception-date       (Instant/now)
                                     :contacting-direction "SENT"
                                     :contact              (map osapuoli->contact osapuolet)}
                 :document          (toimenpide-type->document (:type-id toimenpide))}
   :rfi-order   {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                     :processing-action {:name-identity "Käsittely"}}
                 :processing-action {:name                 "Kehotuksen antaminen"
                                     :reception-date       (Instant/now)
                                     :contacting-direction "SENT"
                                     :contact              (map osapuoli->contact osapuolet)}
                 :document          (toimenpide-type->document (:type-id toimenpide))}
   :rfi-warning {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                     :processing-action {:name-identity "Käsittely"}}
                 :processing-action {:name                 "Varoituksen antaminen"
                                     :reception-date       (Instant/now)
                                     :contacting-direction "SENT"
                                     :contact              (map osapuoli->contact osapuolet)}
                 :document          (toimenpide-type->document (:type-id toimenpide))}
   :decision-order-hearing-letter {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                                       :processing-action {:name-identity "Päätöksenteko"}}
                                   :document (toimenpide-type->document (:type-id toimenpide))
                                   :processing-action {:name                 "Kuulemiskirje käskypäätöksestä"
                                                       :reception-date       (Instant/now)
                                                       :contacting-direction "SENT"
                                                       :contact              (map osapuoli->contact osapuolet)}}
   :decision-order-actual-decision {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                                        :processing-action {:name-identity "Päätöksenteko"}}
                                    :document (toimenpide-type->document (:type-id toimenpide))
                                    :processing-action {:name                 "Käskypäätös"
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

(defn generate-pdf-document
  [db whoami valvonta toimenpide ilmoituspaikat osapuoli osapuolet roolit]
  (let [template-id (:template-id toimenpide)
        template (-> (valvonta-kaytto-db/select-template db {:id template-id}) first)
        documents (find-kaytto-valvonta-documents db (:id valvonta))
        tiedoksi (if (template/send-tiedoksi? template) (filter osapuoli/tiedoksi? osapuolet) [])
        template-data (template-data db whoami valvonta toimenpide
                                     osapuoli documents ilmoituspaikat
                                     tiedoksi roolit)]
    (pdf/generate-pdf->bytes {:template (:content template)
                              :data     template-data})))

(defn log-toimenpide! [db aws-s3-client whoami valvonta toimenpide osapuolet ilmoituspaikat roolit]
  (let [request-id (request-id (:id valvonta) (:id toimenpide))
        sender-id (:email whoami)
        case-number (:diaarinumero toimenpide)
        processing-action (resolve-processing-action toimenpide osapuolet)
        documents (when (:document processing-action)
                    (->> osapuolet
                         (filter osapuoli/omistaja?)
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
