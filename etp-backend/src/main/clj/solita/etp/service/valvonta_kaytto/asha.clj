(ns solita.etp.service.valvonta-kaytto.asha)

(defn open-case! [db whoami toimenpide])

(defn log-toimenpide! [db aws-s3-client whoami valvonta-id toimenpide])

(defn close-case! [db whoami toimenpide])

(defn generate-template [db valvonta-id toimenpide-id toimenpide])

(defn find-document [aws-s3-client valvonta-id toimenpide-id])