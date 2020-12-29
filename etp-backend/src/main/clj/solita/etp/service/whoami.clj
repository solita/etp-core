(ns solita.etp.service.whoami
  (:require [schema.core :as schema]
            [schema-tools.core :as st]
            [buddy.hashers :as hashers]
            [flathead.flatten :as flat]
            [solita.etp.db :as db]
            [solita.etp.service.json :as json]
            [solita.etp.schema.whoami :as whoami-schema]))

;; *** Require sql functions ***
(db/require-queries 'whoami)

(defn verified-api-key? [api-key api-key-hash]
  (if (and api-key api-key-hash)
    (:valid (hashers/verify api-key api-key-hash))
    false))

(defn- find-whoami-with-api-key-hash [db opts]
  (some->> (merge {:email nil
                   :cognitoid nil
                   :henkilotunnus nil
                   :virtu {:localid nil
                           :organisaatio nil}}
                  opts)
           (flat/tree->flat "_")
           (whoami-db/select-whoami db)
           first
           (flat/flat->tree #"\$")))

(defn find-whoami [db opts]
  (some-> (find-whoami-with-api-key-hash db opts)
          (st/select-schema whoami-schema/Whoami)))

(defn find-whoami-by-email-and-api-key [db email api-key]
  (let [whoami (find-whoami-with-api-key-hash db {:email email})]
    (when (verified-api-key? api-key (:api-key-hash whoami))
      (st/select-schema whoami whoami-schema/Whoami))))

(defn update-kayttaja-with-whoami! [db whoami]
  (whoami-db/update-kayttaja-with-whoami! db whoami))
