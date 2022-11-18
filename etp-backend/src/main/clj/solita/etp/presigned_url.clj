(ns solita.etp.presigned-url
  (:require [solita.etp.signature :as signature]
            [solita.etp.service.json :as json]))

(defn unix-time []
  (quot (System/currentTimeMillis) 1000))

(defn policy-document [url end-time]
  {"Statement" [{"Resource" url
                 "Condition" {"DateLessThan" {"AWS:EpochTime" end-time}}}]})

(defn url->signed-url [url expires {:keys [key-pair-id private-key]}]
  (str url
       "?Expires=" expires
       "&Signature=" (signature/sign-document (-> (policy-document url expires)
                                                  json/write-value-as-string) private-key)
       "&Key-Pair-Id=" key-pair-id))
