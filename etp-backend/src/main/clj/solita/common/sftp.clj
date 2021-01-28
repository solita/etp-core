(ns solita.common.sftp
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import (com.jcraft.jsch JSch ChannelSftp SftpException)))

(def default-port 22)

(defn connect!
  ([host username password known-hosts-path]
   (connect! host default-port username password known-hosts-path))
  ([host port username password known-hosts-path]
   (let [jsch (doto (JSch.)
                (.setKnownHosts (-> known-hosts-path io/resource io/input-stream)))
         session (doto (.getSession jsch username host port)
                   (.setPassword password)
                   (.connect))]
     {:jsch jsch
      :session session
      :channel (doto (.openChannel session "sftp")
                 (.connect))})))

(defn disconnect! [{:keys [session channel]}]
  (.disconnect channel)
  (.disconnect session)
  nil)

(defn connected? [{:keys [session]}]
  (.isConnected session))

(defn list-files
  ([{:keys [channel] :as connection}]
   (list-files connection (.pwd channel)))
  ([{:keys [channel]} path]
   (->> (if (str/blank? path)
          (.pwd channel)
          path)
        (.ls channel)
        (map #(.getFilename %))
        (remove #(contains? #{"." ".."} %))
        (set))))

(defn file-exists? [connection path]
  (let [split-path (str/split path #"/")
        dir-path (->> split-path butlast (str/join "/"))]
    (contains? (list-files connection dir-path) (last split-path))))

(defn make-directory! [{:keys [channel] :as connection} path]
  (when-not (file-exists? connection path)
    (.mkdir channel path)))

(defn upload! [{:keys [channel]} src-path dst-path]
  (.put channel src-path dst-path))

(defn delete!
  ([connection path]
   (delete! connection path false))
  ([{:keys [channel] :as connection} path directory?]
   (when (file-exists? connection path)
     (if directory?
       (.rmdir channel path)
       (.rm channel path)))))
