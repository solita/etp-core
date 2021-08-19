(ns solita.etp.service.valvonta-kaytto.asha
  (:require [solita.common.time :as time]
            [solita.etp.service.asha :as asha]
            [solita.etp.service.valvonta-kaytto.toimenpide :as toimenpide]
            [solita.etp.schema.valvonta-kaytto :as kaytto-schema]
            [solita.etp.service.pdf :as pdf]
            [solita.etp.db :as db]
            [solita.common.formats :as formats]
            [solita.etp.service.file :as file-service]))

(db/require-queries 'valvonta-kaytto)
(db/require-queries 'geo)

(def file-key-prefix "valvonta/kaytto")

(defn toimenpide-type->document [type-id]
  (let [type-key (toimenpide/type-key type-id )
        documents {:rfi-request {:type "Pyyntö" :filename "tietopyynto.pdf"}
                   :rfi-order {:type "Kirje" :filename "kehotus_tietopyynto.pdf"}
                   :rfi-warning {:type "Kirje" :filename "varoitus_tietopyynto.pdf"}}]
    (get documents type-key)))

(defn- file-path [file-key-prefix valvonta-id toimenpide-id osapuoli]
  (cond
    (kaytto-schema/henkilo? osapuoli) (str file-key-prefix "/" valvonta-id "/" toimenpide-id "/henkilo/" (:id osapuoli))
    (kaytto-schema/yritys? osapuoli) (str file-key-prefix "/" valvonta-id "/" toimenpide-id "/yritys/" (:id osapuoli))))

(defn store-document [aws-s3-client valvonta-id toimenpide-id osapuoli document]
  (file-service/upsert-file-from-bytes
    aws-s3-client
    (file-path file-key-prefix valvonta-id toimenpide-id osapuoli)
    document))

(defn find-document [aws-s3-client valvonta-id toimenpide-id osapuoli]
  (file-service/find-file aws-s3-client (file-path file-key-prefix valvonta-id toimenpide-id osapuoli)))

(defn find-kaytto-valvonta-documents [db valvonta-id]
  (->> (valvonta-kaytto-db/select-valvonta-documents db {:valvonta-id valvonta-id})
       (map (fn [toimenpide]
              (let [type (toimenpide/type-key (:type-id toimenpide))]
                {type (:publish-time toimenpide)})))
       (into {})))

(defn- find-ilmoituspaikka [ilmoituspaikat ilmoituspaikka-id]
  (->> ilmoituspaikat (filter #(= (:id %) ilmoituspaikka-id)) first :label-fi))

(defn- find-postitoimipaikka [db postinumero]
  (-> (geo-db/select-postinumero-by-id db {:id (formats/string->int postinumero)}) first :label-fi ))

(defn- template-data [db whoami valvonta toimenpide osapuoli dokumentit ilmoituspaikat]
  {:päivä            (time/today)
   :määräpäivä       (time/format-date (:deadline-date toimenpide))
   :diaarinumero     (:diaarinumero toimenpide)
   :valvoja          (select-keys whoami [:etunimi :sukunimi :email])
   :omistaja-henkilo (when (kaytto-schema/henkilo? osapuoli)
                       {:etunimi          (:etunimi osapuoli)
                        :sukunimi         (:sukunimi osapuoli)
                        :katuosoite       (:jakeluosoite osapuoli)
                        :postinumero      (:postinumero osapuoli)
                        :postitoimipaikka (find-postitoimipaikka db (:postinumero osapuoli))})
   :omistaja-yritys  (when (kaytto-schema/yritys? osapuoli)
                       {:nimi             (:nimi osapuoli)
                        :ytunnus          (:ytunnus osapuoli)
                        :katuosoite       (:jakeluosoite osapuoli)
                        :postinumero      (:postinumero osapuoli)
                        :postitoimipaikka (find-postitoimipaikka db (:postinumero osapuoli))})
   :kohde            {:nimi           (:rakennustunnus valvonta)
                      :ilmoituspaikka (find-ilmoituspaikka ilmoituspaikat (:ilmoituspaikka-id valvonta))
                      :ilmoitustunnus (:ilmoitustunnus valvonta)
                      :havaintopäivä  (-> valvonta :havaintopaiva time/format-date)}
   :toimituspyyntö   {:toimituspyyntö-pvm         (time/format-date (:rfi-request dokumentit))
                      :toimituspyyntö-kehotus-pvm (time/format-date (:rfi-order dokumentit))}})

(defn generate-template [db whoami valvonta toimenpide osapuoli ilmoituspaikat]
  (let [template (-> (valvonta-kaytto-db/select-template db {:id (:template-id toimenpide)}) first :content)
        dokumentit (find-kaytto-valvonta-documents db (:id valvonta))
        template-data (template-data db whoami valvonta toimenpide osapuoli dokumentit ilmoituspaikat)]
    {:template      template
     :template-data template-data}))

(defn- request-id [valvonta-id toimenpide-id]
  (str valvonta-id "/" toimenpide-id))

(defn- osapuoli->contact [osapuoli]
  (cond
    (kaytto-schema/henkilo? osapuoli)
    {:type          "ORGANIZATION"                          ;No enum constant fi.ys.eservice.entity.ContactType.PERSON
     :first-name    (:etunimi osapuoli)
     :last-name     (:sukunimi osapuoli)
     :phone-number  (:puhelin osapuoli)
     :email-address (:email osapuoli)}
    (kaytto-schema/yritys? osapuoli)
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

                    :description    (asha/string-join "\r" [(find-ilmoituspaikka ilmoituspaikat (:ilmoituspaikka-id valvonta))
                                                            (:ilmoitustunnus valvonta)])
                    :attach         {:contact (map osapuoli->contact osapuolet)}}))

(defn log-toimenpide! [db aws-s3-client whoami valvonta toimenpide osapuolet ilmoituspaikat]
  (let [request-id (request-id (:id valvonta) (:id toimenpide))
        sender-id (:email whoami)
        case-number (:diaarinumero toimenpide)
        processing-action (resolve-processing-action toimenpide osapuolet)
        documents (when (:document processing-action)
                    (map (fn [osapuoli]
                           (let [{:keys [template template-data]} (generate-template db whoami valvonta toimenpide osapuoli ilmoituspaikat)
                                 bytes (pdf/generate-pdf->bytes template template-data)]
                             (store-document aws-s3-client (:id valvonta) (:id toimenpide) osapuoli bytes)
                             bytes)) osapuolet))]
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