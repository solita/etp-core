(ns solita.etp.service.suomifi-viestit-test
  (:require [clojure.test :as t]
            [solita.etp.test-system :as ts]
            [clojure.java.io :as io]
            [solita.etp.service.suomifi-viestit :as suomifi-viestit]
            [clojure.string :as str]
            [solita.etp.service.pdf :as pdf]))

(t/use-fixtures :each ts/fixture)

(defn- handle-request [request-resource response-resource response-status]
  (fn [request]
    (t/is (= (-> request str/trim) (-> request-resource io/resource slurp str/trim)))
    {:body   (-> response-resource io/resource slurp)
     :status response-status}))

(t/deftest send-message-to-osapuoli-test
  (with-bindings {#'suomifi-viestit/make-send-requst! (handle-request "suomifi/viesti-request.xml"
                                                                      "suomifi/viesti-response.xml"
                                                                      200)
                  #'suomifi-viestit/now               (fn []
                                                        "2021-09-08T06:21:03.625667Z")
                  #'suomifi-viestit/bytes->base64     (fn [_]
                                                        "dGVzdGk=")}
    (t/is (= (suomifi-viestit/send-message-to-osapuoli!
               {:type-id      1
                :id           2
                :valvonta-id  1
                :diaarinumero "ARA-05.03.02-2021-31"}
               {:id               1
                :etunimi          "Testi"
                :sukunimi         "Vastaanottaja"
                :henkilotunnus    "010120-3319"
                :jakeluosoite     "Testitie 1 A"
                :postinumero      "00000"
                :postitoimipaikka "Kaupunki"
                :maa              "FI"}
              (pdf/generate-pdf->bytes {:layout "pdf/ipost-address-page.html"})
               {:viranomaistunnus    "Organisaatio"
                :palvelutunnus       "OR"
                :tulostustoimittaja  "Edita"
                :varmenne            "OR"
                :yhteyshenkilo-nimi  "Henkilö"
                :yhteyshenkilo-email "testi.kayttaja@organisaatio.or"})
             {:tila-koodi        202,
              :tila-koodi-kuvaus "Asia tallennettuna asiointitilipalvelun käsittelyjonoon, mutta se ei vielä näy asiakkaan asiointi-tilillä. Lopullinen vastaus on haettavissa erikseen erillisellä kutsulla."
              :sanoma-tunniste   "ETP-1-2-1"}))))