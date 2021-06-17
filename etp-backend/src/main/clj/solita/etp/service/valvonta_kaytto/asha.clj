(ns solita.etp.service.valvonta-kaytto.asha
  (:require [solita.common.time :as time]
            [solita.etp.service.asha :as asha]
            [clojure.string :as str]
            [solita.etp.service.valvonta-kaytto.toimenpide :as toimenpide]
            [solita.etp.service.pdf :as pdf]
            [clojure.java.io :as io]
            [solita.etp.db :as db]
            [solita.etp.service.geo :as geo]
            [solita.common.formats :as formats]))

#_(db/require-queries 'valvonta-kaytto)
(db/require-queries 'geo)

(def file-key-prefix "valvonta/kaytto")

(defn toimenpide-type->document [type-id]
  (let [type-key (toimenpide/type-key type-id )
        documents {:rfi-request {:type "Pyyntö" :filename "toimituspyynto.pdf"}
                   :rfi-order {:type "Kirje" :filename "kehotus_toimituspyynto.pdf"}
                   :rfi-warning {:type "Kirje" :filename "varoitus_toimituspyynto.pdf"}}]
    (get documents type-key)))

(defn find-document [aws-s3-client valvonta-id toimenpide-id]
  (asha/find-document aws-s3-client file-key-prefix valvonta-id toimenpide-id))
#_
(defn find-kaytto-valvonta-documents [db id]
  (->> (valvonta-oikeellisuus-db/select-kaytto-valvonta-documents db {:valvonta-id id})
       (map (fn [toimenpide]
              (let [type (toimenpide/type-key (:type-id toimenpide))]
                {type (:publish-time toimenpide)})))
       (into {})))

(defn- find-ilmoituspaikka [ilmoituspaikat ilmoituspaikka-id]
  (->> ilmoituspaikat (filter #(= (:id %) ilmoituspaikka-id)) first :label-fi))

(defn- find-postitoimipaikka [db postinumero]
  (-> (geo-db/select-postinumero-by-id db {:id (formats/string->int postinumero)}) first :label-fi ))

(defn- template-data [db whoami valvonta toimenpide henkilo dokumentit ilmoituspaikat]
  {:päivä          (time/today)
   :määräpäivä     (time/format-date (:deadline-date toimenpide))
   :diaarinumero   (:diaarinumero toimenpide)
   :valvoja        (select-keys whoami [:etunimi :sukunimi :email])
   :omistaja       {:etunimi          (:etunimi henkilo)
                    :sukunimi         (:sukunimi henkilo)
                    :katuosoite       (:jakeluosoite henkilo)
                    :postinumero      (:postinumero henkilo)
                    :postitoimipaikka (find-postitoimipaikka db (:postinumero henkilo))}
   :kohde          {:nimi           (:rakennustunnus valvonta)
                    :ilmoituspaikka (find-ilmoituspaikka ilmoituspaikat (:ilmoituspaikka-id valvonta))
                    :ilmoitustunnus (:ilmoitustunnus valvonta)
                    :havaintopäivä  (-> valvonta :havaintopaiva time/format-date)}
   :toimituspyyntö {:toimituspyyntö-pvm         (time/format-date (:rfi-request dokumentit))
                    :toimituspyyntö-kehotus-pvm (time/format-date (:rfi-order dokumentit))}})

(defn template-id->template [template-id]
  (let [file (case template-id
               1 "pdf/toimituspyynto.html"
               2 "pdf/toimituspyynto-kehotus.html"
               3 "pdf/toimituspyynto-varoitus.html"
               "pdf/tietopyynto.html")]
    (-> file io/resource slurp)))

(defn generate-template [db whoami valvonta toimenpide henkilo ilmoituspaikat]
  (let [template (template-id->template (:template-id toimenpide)) #_(:content toimenpide)
        dokumentit {} #_(find-kaytto-valvonta-documents db valvonta-id)
        template-data (template-data db whoami valvonta toimenpide henkilo dokumentit ilmoituspaikat)]
    {:template      template
     :template-data template-data}))

(defn- request-id [valvonta-id toimenpide-id]
  (str valvonta-id "/" toimenpide-id))

(defn- osapuoli->contact [henkilo]
  ; TODO: check yritysosapuoli
  {:type          "ORGANIZATION"                          ;No enum constant fi.ys.eservice.entity.ContactType.PERSON
   :first-name    (:etunimi henkilo)
   :last-name     (:sukunimi henkilo)
   :email-address (:email henkilo)})

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

(defn open-case! [db whoami valvonta henkilot ilmoituspaikat]
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
                    :attach         {:contact (map osapuoli->contact henkilot)}}))

(defn log-toimenpide! [db aws-s3-client whoami valvonta toimenpide osapuolet ilmoituspaikat]
  (let [request-id (request-id (:id valvonta) (:id toimenpide))
        sender-id (:email whoami)
        case-number (:diaarinumero toimenpide)
        processing-action (resolve-processing-action toimenpide osapuolet)
        document (when (:document processing-action)
                   (let [{:keys [template template-data]} (generate-template db whoami valvonta toimenpide (first osapuolet) ilmoituspaikat) ; TODO: fix to get all henkilot
                         bytes (pdf/generate-pdf->bytes template template-data)]
                     (asha/store-document aws-s3-client file-key-prefix (:id valvonta) (:id toimenpide) bytes)
                     bytes))]
    (asha/log-toimenpide!
      sender-id
      request-id
      case-number
      processing-action
      document)))

(defn close-case! [whoami valvonta-id toimenpide]
  (asha/close-case!
    (:email whoami)
    (request-id valvonta-id (:id toimenpide))
    (:diaarinumero toimenpide)
    (:description toimenpide)))