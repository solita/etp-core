<<<<<<< HEAD
(ns solita.etp.service.valvonta-kaytto.asha
  (:require [solita.common.time :as time]
            [solita.etp.service.asha :as asha]
            [clojure.string :as str]
            [solita.etp.service.valvonta-oikeellisuus.toimenpide :as toimenpide]
            [solita.etp.service.pdf :as pdf]
            [clojure.java.io :as io]
            [solita.etp.db :as db]))

#_(db/require-queries 'valvonta-kaytto)

(def file-key-prefix "valvonta/kaytto")

(defn toimenpide-type->document [type-id]
  (let [type-key (toimenpide/type-key type-id )
        documents {:rfi-request {:type "Pyyntö" :filename "toimituspyynto.pdf"}
                   :rfi-order {:type "Kirje" :filename "kehotus_toimituspyynto.pdf"}
                   :rfi-warning {:type "Kirje" :filename "varoitus_toimituspyynto.pdf"}}]
    (get documents type-key)))

(defn find-document [aws-s3-client valvonta-id toimenpide-id]
  (asha/find-document aws-s3-client file-key-prefix valvonta-id toimenpide-id))

(defn find-kaytto-valvonta-documents [db id]
  (->> (valvonta-oikeellisuus-db/select-kaytto-valvonta-documents db {:valvonta-id id})
       (map (fn [toimenpide]
              (let [type (toimenpide/type-key (:type-id toimenpide))]
                {type (:publish-time toimenpide)})))
       (into {})))

(defn- template-data [whoami toimenpide dokumentit]
  {:päivä          (time/today)
   :määräpäivä     (time/format-date (:deadline-date toimenpide))
   :diaarinumero   (:diaarinumero toimenpide)
   :valvoja        (select-keys whoami [:etunimi :sukunimi :email])
   :omistaja       (:omistaja toimenpide)
   :kohde          (update (:kohde toimenpide) :havaintopäivä time/format-date)
   :toimituspyyntö {:toimituspyyntö-pvm         (time/format-date (:rfi-request dokumentit))
                    :toimituspyyntö-kehotus-pvm (time/format-date (:rfi-order dokumentit))}})

(defn template-id->template [template-id]
  (let [file (case template-id
               1 "pdf/toimituspyynto.html"
               2 "pdf/toimituspyynto-kehotus.html"
               3 "pdf/toimituspyynto-varoitus.html"
               "pdf/tietopyynto.html")]
    (-> file io/resource slurp)))

(defn generate-template [db whoami toimenpide]
  (let [template (template-id->template (:template-id toimenpide)) #_(:content toimenpide)
        dokumentit {} #_(find-kaytto-valvonta-documents db valvonta-id)
        template-data (template-data whoami toimenpide dokumentit)]
    {:template      template
     :template-data template-data}))

(defn- request-id [valvonta-id toimenpide-id]
  (str valvonta-id "/" toimenpide-id))

(defn- available-processing-actions [toimenpide]
  {:rfi-request   {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                       :processing-action {:name-identity "Vireillepano"}}
                   :processing-action {:name                 "Tietopyyntö"
                                       :reception-date       (java.time.Instant/now)
                                       :contacting-direction "SENT"
                                       :contact              (asha/kayttaja->contact (:omistaja toimenpide))}
                   :document          (toimenpide-type->document (:type-id toimenpide))}
   :rfi-order     {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                       :processing-action {:name-identity "Käsittely"}}
                   :processing-action {:name                 "Kehotuksen antaminen"
                                       :reception-date       (java.time.Instant/now)
                                       :contacting-direction "SENT"
                                       :contact              (asha/kayttaja->contact (:omistaja toimenpide))}
                   :document          (toimenpide-type->document (:type-id toimenpide))}
   :rfi-warning   {:identity          {:case              {:number (:diaarinumero toimenpide)}
                                       :processing-action {:name-identity "Käsittely"}}
                   :processing-action {:name                 "Varoituksen antaminen"
                                       :reception-date       (java.time.Instant/now)
                                       :contacting-direction "SENT"
                                       :contact              (asha/kayttaja->contact (:omistaja toimenpide))}
                   :document          (toimenpide-type->document (:type-id toimenpide))}})

(defn- resolve-processing-action [toimenpide]
  (let [processing-actions (available-processing-actions toimenpide)
        type-key (toimenpide/type-key (:type-id toimenpide))]
    (get processing-actions type-key)))

(defn- string-join [separator coll]
  (str/join separator (->> coll
                           (map str)
                           (remove empty?))))

(defn open-case! [whoami valvonta-id omistajat kohde]
  (asha/open-case! {:request-id     (request-id valvonta-id 1)
                    :sender-id      (:email whoami)
                    :classification "05.03.01"
                    :service        "general"             ; Yleinen menettely
                    :name           (string-join ", " [(-> kohde :rakennustunnus)
                                                       (-> kohde :katuosoite-fi)
                                                       (string-join " " [(-> kohde :postinumero)
                                                                         (-> kohde :postitoimipaikka-fi)])])

                    :description    (string-join "\r" [(:ilmoituspaikka kohde)
                                                       (:kohdenumero kohde)])
                    :attach         {:contact (map #(asha/kayttaja->contact %) omistajat)}}))

(defn log-toimenpide! [db aws-s3-client whoami valvonta-id toimenpide]
  (let [request-id (request-id valvonta-id (:id toimenpide))
        sender-id (:email whoami)
        case-number (:diaarinumero toimenpide)
        processing-action (resolve-processing-action toimenpide)
        document (when (:document processing-action)
                   (let [{:keys [template template-data]} (generate-template db whoami toimenpide)
                         bytes (pdf/generate-pdf->bytes template template-data)]
                     (asha/store-document aws-s3-client file-key-prefix valvonta-id (:id toimenpide) bytes)
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