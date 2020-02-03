(ns solita.etp.db
  (:require [integrant.core :as ig]
            [hikari-cp.core :as hikari]))

(defmethod ig/init-key :solita.etp/db
  [_ opts]
  (assoc opts :datasource (hikari/make-datasource opts)))

(defmethod ig/halt-key! :solita.etp/db
  [_ {:keys [datasource]}]
  (hikari/close-datasource datasource))
