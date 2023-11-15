(ns solita.etp.service.valvonta-kaytto.toimenpide-type-specific-data
  (:require [solita.etp.db :as db]
            [solita.etp.exception :as exception]
            [solita.etp.service.valvonta-kaytto.toimenpide :as toimenpide]))

(db/require-queries 'hallinto-oikeus)
(db/require-queries 'karajaoikeus)

(defmulti format-type-specific-data
          (fn [_db toimenpide _osapuoli-id _osapuoli-type] (-> toimenpide :type-id toimenpide/type-key)))

(defn- find-value-from-osapuoli-specific-data [key osapuoli-specific-data osapuoli-id osapuoli-type]
  (->> osapuoli-specific-data
       (filter #(= ((juxt :osapuoli-id :osapuoli-type) %)
                   [osapuoli-id osapuoli-type]))
       first
       key))

(def find-administrative-court-id-from-osapuoli-specific-data
  (partial find-value-from-osapuoli-specific-data :hallinto-oikeus-id))

(def find-recipient-answered-from-osapuoli-specific-data
  (partial find-value-from-osapuoli-specific-data :recipient-answered))

(defn hallinto-oikeus-id->formatted-strings [db hallinto-oikeus-id]
  (if-let [formatted-strings (first (hallinto-oikeus-db/find-document-template-wording-by-hallinto-oikeus-id db {:hallinto-oikeus-id hallinto-oikeus-id}))]
    formatted-strings
    (exception/throw-ex-info!
      {:message (str "Unknown hallinto-oikeus-id: " hallinto-oikeus-id)})))

(defn format-actual-decision-data [db toimenpide osapuoli-id osapuoli-type]
  (let [recipient-answered? (-> toimenpide
                                :type-specific-data
                                :osapuoli-specific-data
                                (find-recipient-answered-from-osapuoli-specific-data osapuoli-id osapuoli-type))
        hallinto-oikeus-strings (hallinto-oikeus-id->formatted-strings
                                  db
                                  (-> toimenpide
                                      :type-specific-data
                                      :osapuoli-specific-data
                                      (find-administrative-court-id-from-osapuoli-specific-data osapuoli-id osapuoli-type)))]
    {:recipient-answered       recipient-answered?
     :vastaus-fi               (let [answer-commentary (-> toimenpide
                                                           :type-specific-data
                                                           :osapuoli-specific-data
                                                           ((partial find-value-from-osapuoli-specific-data :answer-commentary-fi) osapuoli-id osapuoli-type))
                                     recipient-answered-string (if recipient-answered?
                                                                 "Asianosainen antoi vastineen kuulemiskirjeeseen. "
                                                                 "Asianosainen ei vastannut kuulemiskirjeeseen. ")]
                                 (str recipient-answered-string answer-commentary))
     :vastaus-sv               (let [answer-commentary (-> toimenpide
                                                           :type-specific-data
                                                           :osapuoli-specific-data
                                                           ((partial find-value-from-osapuoli-specific-data :answer-commentary-sv) osapuoli-id osapuoli-type))
                                     recipient-answered-string (if recipient-answered?
                                                                 "gav ett bemötande till brevet om hörande. "
                                                                 "svarade inte på brevet om hörande. ")]
                                 (str recipient-answered-string answer-commentary))
     :oikeus-fi                (:fi hallinto-oikeus-strings)
     :oikeus-sv                (:sv hallinto-oikeus-strings)
     :fine                     (-> toimenpide :type-specific-data :fine)
     :statement-fi             (-> toimenpide
                                   :type-specific-data
                                   :osapuoli-specific-data
                                   ((partial find-value-from-osapuoli-specific-data :statement-fi) osapuoli-id osapuoli-type))
     :statement-sv             (-> toimenpide
                                   :type-specific-data
                                   :osapuoli-specific-data
                                   ((partial find-value-from-osapuoli-specific-data :statement-sv) osapuoli-id osapuoli-type))
     :department-head-name     (-> toimenpide :type-specific-data :department-head-name)
     :department-head-title-fi (-> toimenpide :type-specific-data :department-head-title-fi)
     :department-head-title-sv (-> toimenpide :type-specific-data :department-head-title-sv)}))

(defmethod format-type-specific-data :decision-order-actual-decision [db toimenpide osapuoli-id osapuoli-type]
  (format-actual-decision-data db toimenpide osapuoli-id osapuoli-type))

(defmethod format-type-specific-data :penalty-decision-actual-decision [db toimenpide osapuoli-id osapuoli-type]
  (format-actual-decision-data db toimenpide osapuoli-id osapuoli-type))

(defn- karajaoikeus-id->name [db id]
  (first (karajaoikeus-db/find-karajaoikeus-name-by-id db {:karajaoikeus-id id})))

(defn- format-notice-bailiff [db toimenpide osapuoli-id osapuoli-type]
  (let [karajaoikeus-id (-> toimenpide
                            :type-specific-data
                            :osapuoli-specific-data
                            ((partial find-value-from-osapuoli-specific-data :karajaoikeus-id) osapuoli-id osapuoli-type))
        haastemies-email (-> toimenpide
                             :type-specific-data
                             :osapuoli-specific-data
                             ((partial find-value-from-osapuoli-specific-data :haastemies-email) osapuoli-id osapuoli-type))]
    {:karajaoikeus     (karajaoikeus-id->name db karajaoikeus-id)
     :haastemies-email haastemies-email}))

(defmethod format-type-specific-data :decision-order-notice-bailiff [db toimenpide osapuoli-id osapuoli-type]
  (format-notice-bailiff db toimenpide osapuoli-id osapuoli-type))

(defmethod format-type-specific-data :penalty-decision-notice-bailiff [db toimenpide osapuoli-id osapuoli-type]
  (format-notice-bailiff db toimenpide osapuoli-id osapuoli-type))

(defmethod format-type-specific-data :default [_ toimenpide _ _]
  (:type-specific-data toimenpide))
