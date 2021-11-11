(ns solita.etp.service.suomifi-viestit-test
  (:require [clojure.test :as t]
            [solita.etp.test-system :as ts]
            [clojure.java.io :as io]
            [solita.etp.service.valvonta-kaytto.suomifi-viestit :as suomifi-viestit]
            [clojure.string :as str]
            [solita.etp.service.pdf :as pdf])
  (:import (java.time LocalDate)))

(t/use-fixtures :each ts/fixture)

(defn- handle-request [request-resource response-resource response-status]
  (fn [request]
    (t/is (= (-> request str/trim) (-> request-resource io/resource slurp str/trim)))
      {:body   (-> response-resource io/resource slurp)
       :status response-status}))

(def valvonta {:id                  1
               :rakennustunnus      "103515074X"
               :katuosoite          "Hämeenkatu 10"
               :postinumero         "333100"
               :postitoimipaikka-fi "Tampere"})

(def toimenpide {:type-id       1
                 :id            2
                 :diaarinumero  "ARA-05.03.02-2021-31"
                 :valvonta-id   1
                 :deadline-date (LocalDate/of 2022 1 1)})

(def osapuoli {:id               1
               :etunimi          "Testi"
               :sukunimi         "Vastaanottaja"
               :henkilotunnus    "010120-3319"
               :jakeluosoite     "Testitie 1 A"
               :postinumero      "00000"
               :postitoimipaikka "Kaupunki"
               :maa              "FI"})

(defonce document (pdf/generate-pdf->bytes {:layout "pdf/ipost-address-page.html"}))

(def config {:viranomaistunnus    "Organisaatio"
             :palvelutunnus       "OR"
             :tulostustoimittaja  "Edita"
             :varmenne            "OR"
             :yhteyshenkilo-nimi  "Henkilö"
             :yhteyshenkilo-email "testi.kayttaja@organisaatio.or"
             :laskutus-tunniste   "0000"
             :laskutus-salasana   "0000"})

(t/deftest send-message-to-osapuoli-test
  (with-bindings {#'solita.etp.service.suomifi-viestit/post! (handle-request "suomifi/viesti-request.xml"
                                                                             "suomifi/viesti-response.xml"
                                                                             202)
                  #'suomifi-viestit/now                      (fn [] "2021-09-08T06:21:03.625667Z")
                  #'suomifi-viestit/bytes->base64            (fn [_] "dGVzdGk=")}

    (t/is (= (:sanoma-tunniste (suomifi-viestit/send-message-to-osapuoli! valvonta toimenpide osapuoli document config))
             "ARA-05.03.02-2021-31-ETP-KV-1-2-PERSON-1"))))

(t/deftest send-message-to-osapuoli-id-already-exists-test
  (with-bindings {#'solita.etp.service.suomifi-viestit/post! (handle-request "suomifi/viesti-request.xml"
                                                                             "suomifi/viesti-id-already-exists-response.xml"
                                                                             200)
                  #'suomifi-viestit/now                      (fn []
                                                                            "2021-09-08T06:21:03.625667Z")
                  #'suomifi-viestit/bytes->base64            (fn [_]
                                                                            "dGVzdGk=")}
    (t/is (thrown-with-msg?
            clojure.lang.ExceptionInfo
            #"Sending suomifi message ARA-05.03.02-2021-31-ETP-KV-1-2-PERSON-1 failed."
            (suomifi-viestit/send-message-to-osapuoli! valvonta toimenpide osapuoli document config)))))