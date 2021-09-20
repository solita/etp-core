(ns solita.etp.service.valvonta-kaytto.store
  (:require [solita.etp.service.file :as file-service]
            [solita.etp.schema.valvonta-kaytto :as kaytto-schema]))

(def file-key-prefix "valvonta/kaytto")

(defn- file-path [file-key-prefix valvonta-id toimenpide-id osapuoli]
  (cond
    (kaytto-schema/henkilo? osapuoli) (str file-key-prefix "/" valvonta-id "/" toimenpide-id "/henkilo/" (:id osapuoli))
    (kaytto-schema/yritys? osapuoli) (str file-key-prefix "/" valvonta-id "/" toimenpide-id "/yritys/" (:id osapuoli))))

(defn store-document [aws-s3-client valvonta-id toimenpide-id osapuoli document]
  (file-service/upsert-file-from-bytes
    aws-s3-client
    (file-path file-key-prefix valvonta-id toimenpide-id osapuoli)
    document))

(defn find-document [aws-s3-client valvonta-id toimenpide-id osapuoli]
  (file-service/find-file aws-s3-client (file-path file-key-prefix valvonta-id toimenpide-id osapuoli)))

