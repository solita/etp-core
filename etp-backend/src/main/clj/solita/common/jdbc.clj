(ns solita.common.jdbc
  (:require [clojure.java.jdbc :as jdbc])
  (:import (java.sql Connection)))

(defn get-connection [original-get-connection & args]
  (let [^Connection connection (apply original-get-connection args)
        application-name (-> args first :application-name)]
    (when-not (nil? application-name)
      (.setClientInfo connection "ApplicationName" application-name))
    connection))

(defn with-application-name-support [db-function]
  (let [original-get-connection jdbc/get-connection]
    (with-redefs [jdbc/get-connection
                  (partial get-connection original-get-connection)]
      (db-function))))