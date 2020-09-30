(ns solita.etp.service.geo
  (:require [solita.etp.db :as db]
            [solita.etp.service.json :as json]
            [solita.etp.schema.geo :as geo-schema]
            [schema.coerce :as coerce]))

; *** Require sql functions ***
(db/require-queries 'geo)

(defn find-all-countries [db]
  (map db/kebab-case-keys (geo-db/select-countries db)))

(defn find-all-toiminta-alueet [db]
  (map db/kebab-case-keys (geo-db/select-toiminta-alueet db)))

(defn find-all-postinumerot [db]
  (map db/kebab-case-keys (geo-db/select-postinumerot db)))
