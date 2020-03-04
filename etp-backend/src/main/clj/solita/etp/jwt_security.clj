(ns solita.etp.jwt-security
  (:require [clojure.string :as str]
            [buddy.core.codecs :as codecs]
            [buddy.core.codecs.base64 :as base64]

            ;; TODO json namespace should probably not be
            ;; under service namespace
            [solita.etp.service.json :as json]))

#_(def trusted-issuers #{"https://cognito-idp.eu-central-1.amazonaws.com/eu-central-1_qUrLSca82"})

(defn decode-jwt-section [jwt-s-section]
  (try
    (->> jwt-s-section base64/decode codecs/bytes->str json/read-value)
    (catch Exception e (do (.printStackTrace e)
                           nil))))

(defn decoded-jwt [jwt-s]
  (let [[header payload signature :as all] (str/split jwt-s #"\.")]
    (when (= (count all) 3)
      {:header (decode-jwt-section header)
       :payload (decode-jwt-section payload)
       :signature signature})))
