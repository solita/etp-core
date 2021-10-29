(ns solita.etp.service.valvonta-kaytto.asha
  (:require [solita.common.time :as time]
            [solita.etp.service.asha :as asha]
            [solita.etp.service.valvonta-kaytto.toimenpide :as toimenpide]
            [solita.etp.service.valvonta-kaytto.store :as store]
            [solita.etp.service.valvonta-kaytto.osapuoli :as osapuoli]
            [solita.etp.service.pdf :as pdf]
            [solita.etp.db :as db]
            [solita.common.formats :as formats]))

(db/require-queries 'valvonta-kaytto)
(db/require-queries 'geo)

(defn toimenpide-type->document [type-id]
  (let [type-key (toimenpide/type-key type-id )
        documents {:rfi-request {:type "Pyyntö" :filename "tietopyynto.pdf"}
                   :rfi-order {:type "Kirje" :filename "kehotus.pdf"}
                   :rfi-warning {:type "Kirje" :filename "varoitus.pdf"}}]
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

(defn- template-data [db whoami valvonta toimenpide osapuoli dokumentit ilmoituspaikat tiedoksi]
  {:päivä            (time/today)
   :määräpäivä       (time/format-date (:deadline-date toimenpide))
   :diaarinumero     (:diaarinumero toimenpide)
   :valvoja          (select-keys whoami [:etunimi :sukunimi :email])
   :omistaja-henkilo (when (osapuoli/henkilo? osapuoli)
                       {:etunimi          (:etunimi osapuoli)
                        :sukunimi         (:sukunimi osapuoli)})
   :omistaja-yritys  (when (osapuoli/yritys? osapuoli)
                       {:nimi             (:nimi osapuoli)})
   :kohde            {:katuosoite       (:katuosoite valvonta)
                      :postinumero      (:postinumero valvonta)
                      :postitoimipaikka (find-postitoimipaikka db (:postinumero valvonta))
                      :ilmoituspaikka   (find-ilmoituspaikka ilmoituspaikat valvonta)
                      :ilmoitustunnus   (:ilmoitustunnus valvonta)
                      :havaintopäivä    (-> valvonta :havaintopaiva time/format-date)}
   :tietopyynto      {:tietopyynto-pvm         (time/format-date (:rfi-request dokumentit))
                      :tietopyynto-kehotus-pvm (time/format-date (:rfi-order dokumentit))}
   :tiedoksi         (map (fn [o]
                            (cond
                              (osapuoli/henkilo? o) (str (:etunimi o) " " (:sukunimi o))
                              (osapuoli/yritys? o) (:nimi o))) tiedoksi)})

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
                                     :reception-date       (java.time.Instant/now)
                                     :contacting-direction "SENT"
                                     :contact              (map osapuoli->contact osapuolet)}
                 :document          (toimenpide-type->document (:type-id toimenpide))}
   :rfi-order   {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                     :processing-action {:name-identity "Käsittely"}}
                 :processing-action {:name                 "Kehotuksen antaminen"
                                     :reception-date       (java.time.Instant/now)
                                     :contacting-direction "SENT"
                                     :contact              (map osapuoli->contact osapuolet)}
                 :document          (toimenpide-type->document (:type-id toimenpide))}
   :rfi-warning {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                     :processing-action {:name-identity "Käsittely"}}
                 :processing-action {:name                 "Varoituksen antaminen"
                                     :reception-date       (java.time.Instant/now)
                                     :contacting-direction "SENT"
                                     :contact             (map osapuoli->contact osapuolet)}
                 :document          (toimenpide-type->document (:type-id toimenpide))}})

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
  [db whoami valvonta toimenpide ilmoituspaikat osapuoli osapuolet]
  (let [template-id (:template-id toimenpide)
        template (-> (valvonta-kaytto-db/select-template db {:id template-id}) first :content)
        documents (find-kaytto-valvonta-documents db (:id valvonta))
        tiedoksi (filter osapuoli/tiedoksi? osapuolet)]
    (let [template-data (template-data db whoami valvonta toimenpide osapuoli documents ilmoituspaikat tiedoksi)]
      (pdf/generate-pdf->bytes {:template template
                                :data     template-data}))))

(defn log-toimenpide! [db aws-s3-client whoami valvonta toimenpide osapuolet ilmoituspaikat]
  (let [request-id (request-id (:id valvonta) (:id toimenpide))
        sender-id (:email whoami)
        case-number (:diaarinumero toimenpide)
        processing-action (resolve-processing-action toimenpide osapuolet)
        documents (when (:document processing-action)
                    (->> osapuolet
                         (filter osapuoli/omistaja?)
                         (map (fn [osapuoli]
                                (let [document (generate-pdf-document db whoami valvonta toimenpide ilmoituspaikat osapuoli osapuolet)]
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