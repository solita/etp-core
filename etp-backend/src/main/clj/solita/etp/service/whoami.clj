(ns solita.etp.service.whoami
  (:require [schema.coerce :as coerce]
            [solita.etp.db :as db]
            [solita.etp.service.json :as json]
            [solita.etp.schema.whoami :as whoami-schema]))

;; *** Require sql functions ***
(db/require-queries 'whoami)

;; *** Conversions from database data types ***
(def coerce-whoami (coerce/coercer whoami-schema/Whoami json/json-coercions))

(defn find-whoami [db opts]
  (->> (merge {:email nil
               :cognitoid nil
               :henkilotunnus nil
               :virtulocalid nil
               :virtuorganisaatio nil}
              opts)
       (whoami-db/select-whoami db)
       (map coerce-whoami)
       first))

(defn update-kayttaja-with-whoami! [db whoami]
  (whoami-db/update-kayttaja-with-whoami! db whoami))
