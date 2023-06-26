(ns solita.etp.valvonta-test
  (:require
    [clojure.java.io :as io]
    [clojure.java.jdbc :as jdbc]
    [clojure.test :as t]
    [jsonista.core :as j]
    [ring.mock.request :as mock]
    [solita.etp.handler :as handler]
    [solita.etp.service.pdf :as pdf]
    [solita.etp.schema.valvonta-kaytto :as valvonta-schema]
    [solita.etp.service.valvonta-kaytto :as valvonta-service]
    [solita.etp.test-data.generators :as generators]
    [solita.etp.test-data.kayttaja :as test-kayttajat]
    [solita.etp.test-system :as ts])
  (:import (java.time LocalDate ZoneId)))

(t/use-fixtures :each ts/fixture)

(defn- handler [req]
  ; Mimics real handler usage with test assets
  (handler/handler (merge req {:db ts/*db* :aws-s3-client ts/*aws-s3-client*})))


(t/deftest adding-toimenpide
  (t/testing "Kun uusi toimenpide lisätään, omistajille joilla on Suomi.fi-viestit käytössä lähtee viesti oikealla kuvauksella"
    (let [suomifi-message-sent (promise)
          assert-suomifi-message-sent (fn [sanoma kohde & _]
                                        (t/is (= {:tunniste "ARA-ETP-KV-1-1-PERSON-1"
                                                  :versio   "1.0"
                                                  }
                                                 sanoma
                                                 ))
                                        (t/is (= (str "Tämän viestin liitteenä on tietopyyntö koskien rakennustasi: 1035150826\n"
                                                      "katu, 65100 VAASA\n"
                                                      "Kehotamme vastaamaan tietopyyntöön 10.11.2024 mennessä.\n\n"
                                                      "Som bilaga till detta meddelande finns en begäran om information som gäller din byggnad: 1035150826\n"
                                                      "katu, 65100 VASA\n"
                                                      "Vi uppmanar dig att besvara begäran om information senast den 10.11.2024.")
                                                 (:kuvaus-teksti kohde))
                                              )
                                        (deliver suomifi-message-sent true))]
      (with-redefs [solita.etp.service.suomifi-viestit/send-message! assert-suomifi-message-sent]
        (let [kayttaja-id (first (test-kayttajat/insert!
                                   (->> (test-kayttajat/generate-adds 1)
                                        (map #(merge %1 {:virtu {:localid "vvirkamies" :organisaatio "testivirasto.fi"}
                                                         :rooli 2
                                                         }))
                                        )))
              valvonta-id (valvonta-service/add-valvonta! ts/*db*
                                                          (-> {} (generators/complete valvonta-schema/ValvontaSave)
                                                              (merge {
                                                                      :rakennustunnus    "1035150826"
                                                                      :katuosoite        "katu"
                                                                      :postinumero       "65100"
                                                                      :valvoja-id        kayttaja-id
                                                                      :ilmoituspaikka-id nil
                                                                      :havaintopaiva     (LocalDate/of 2023 6 1)
                                                                      })))
              _ (valvonta-service/add-henkilo! ts/*db* valvonta-id (-> (generators/complete {} valvonta-schema/HenkiloSave)
                                                                       (merge {:henkilotunnus   (first (generators/unique-henkilotunnukset 1))
                                                                               :rooli-id        0 ; Omistaja
                                                                               :toimitustapa-id 0 ; Suomi-fi
                                                                               })
                                                                       ))
              body {:type-id       2
                    :deadline-date (.toString (LocalDate/of 2024 11 10))
                    :template-id   2
                    :description   "Tee jotain"
                    }
              response (handler (-> (mock/request :post (format "/api/private/valvonta/kaytto/%s/toimenpiteet" valvonta-id))
                                    (mock/json-body body)
                                    (mock/header "x-amzn-oidc-accesstoken" "eyJraWQiOiJ0ZXN0LWtpZCIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJwYWFrYXl0dGFqYUBzb2xpdGEuZmkiLCJ0b2tlbl91c2UiOiJhY2Nlc3MiLCJzY29wZSI6Im9wZW5pZCIsImF1dGhfdGltZSI6MTU4MzIzMDk2OSwiaXNzIjoiaHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL3NvbGl0YS9ldHAtY29yZS9mZWF0dXJlL0FFLTQzLWF1dGgtaGVhZGVycy1oYW5kbGluZy9ldHAtYmFja2VuZC9zcmMvbWFpbi9yZXNvdXJjZXMiLCJleHAiOjE4OTM0NTYwMDAsImlhdCI6MTU4MzQxMzQyNCwidmVyc2lvbiI6MiwianRpIjoiNWZkZDdhMjktN2VlYS00ZjNkLWE3YTYtYzIyODQyNmY2MTJiIiwiY2xpZW50X2lkIjoidGVzdC1jbGllbnRfaWQiLCJ1c2VybmFtZSI6InRlc3QtdXNlcm5hbWUifQ.PY5_jWcdxhCyn2EpFpss7Q0R3_xH1PvHi4mxDLorpppHnciGT2kFLeutebi7XeLtTYwmttTxxg2tyUyX0_UF7zj_P-tdq-kZQlud1ENmRaUxLXO5mTFKXD7zPb6BPFNe0ewRQ7Uuv3lDk_IxOf-6i86VDYB8luyesEXq7ra4S4l8akFodW_QYBSZQnUva_CVyzsTNcmgGTyrz2NI6seT1x6Pt1uFdYI97FHKlCCWVL1Z042omfujfta8j8XkTWdhKf3dfsHRWjrw31xqOkgD7uwPKcrC0U-wIj3U0uX0Rz2Tk4T-kIq4XTkKttYpkJqOmMFAYuhk6MDjfRkPWBZhUA")
                                    (mock/header "x-amzn-oidc-identity" "paakayttaja@solita.fi")
                                    (mock/header "x-amzn-oidc-data" "eyJ0eXAiOiJKV1QiLCJraWQiOiJ0ZXN0LWtpZCIsImFsZyI6IlJTMjU2IiwiaXNzIjoidGVzdC1pc3MiLCJjbGllbnQiOiJ0ZXN0LWNsaWVudCIsInNpZ25lciI6InRlc3Qtc2lnbmVyIiwiZXhwIjoxODkzNDU2MDAwfQ.eyJzdWIiOiJwYWFrYXl0dGFqYUBzb2xpdGEuZmkiLCJjdXN0b206VklSVFVfbG9jYWxJRCI6InZ2aXJrYW1pZXMiLCJjdXN0b206VklSVFVfbG9jYWxPcmciOiJ0ZXN0aXZpcmFzdG8uZmkiLCJ1c2VybmFtZSI6InRlc3QtdXNlcm5hbWUiLCJleHAiOjE4OTM0NTYwMDAsImlzcyI6InRlc3QtaXNzIn0.BfuDVOFUReiJd6N05Re6affps_47AA0F5o-g6prmXgAnk4lB1S3k9RpovCFU3-R5Zn0p38QTiwi5dENHCHaj1A6MGHHKeYd7vBZK0VquuBxlIQH-4k1MWLvpYnkK3yuEvfmbRb3jYspCA_4N-AF21cCyjd15RiuIawLCEM0Km1DRgLhXIBta6XCGSRwaRmrT7boDRMp7hUkYPpoakCahMC70sjyuvLE0pjAy1_S09g4SkboentI7WhfsfN4uAHbKy6ViVMfsnwVVvKsM8dXav_a-6PoNGywuUbi8nHt8c20KiB_AzAEYSqxbRX1YBd0UHlYS16LbLtMBTOctCBLDMg")
                                    (mock/header "Accept" "application/json")
                                    ))
              response-body (j/read-value (:body response) j/keyword-keys-object-mapper)
              ]

          (t/is (= (:status response) 201))
          (t/is (= response-body {:id 1}))
          (t/is (deref suomifi-message-sent 5000 false))
          )))))

(t/deftest fetching-valvonta
  (t/testing "Yksittäisen valvonnan hakeminen palauttaa vastauksen")
  (let [
        kayttaja-id (first (test-kayttajat/insert!
                             (->> (test-kayttajat/generate-adds 1)
                                  (map #(merge %1 {:virtu {:localid "vvirkamies" :organisaatio "testivirasto.fi"}
                                                   :rooli 2
                                                   }))
                                  )))
        valvonta-id (valvonta-service/add-valvonta! ts/*db*
                                                    (-> {}
                                                        (generators/complete valvonta-schema/ValvontaSave)
                                                        (merge {
                                                                :ilmoitustunnus             nil
                                                                :rakennustunnus             "1035150826"
                                                                :katuosoite                 "katu"
                                                                :postinumero                "65100"
                                                                :valvoja-id                 kayttaja-id
                                                                :ilmoituspaikka-id          2
                                                                :ilmoituspaikka-description "Netissä"
                                                                :havaintopaiva              (LocalDate/of 2023 6 1)
                                                                })))
        response (handler (-> (mock/request :get (format "/api/private/valvonta/kaytto/%s" valvonta-id))
                              (mock/header "x-amzn-oidc-accesstoken" "eyJraWQiOiJ0ZXN0LWtpZCIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJwYWFrYXl0dGFqYUBzb2xpdGEuZmkiLCJ0b2tlbl91c2UiOiJhY2Nlc3MiLCJzY29wZSI6Im9wZW5pZCIsImF1dGhfdGltZSI6MTU4MzIzMDk2OSwiaXNzIjoiaHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL3NvbGl0YS9ldHAtY29yZS9mZWF0dXJlL0FFLTQzLWF1dGgtaGVhZGVycy1oYW5kbGluZy9ldHAtYmFja2VuZC9zcmMvbWFpbi9yZXNvdXJjZXMiLCJleHAiOjE4OTM0NTYwMDAsImlhdCI6MTU4MzQxMzQyNCwidmVyc2lvbiI6MiwianRpIjoiNWZkZDdhMjktN2VlYS00ZjNkLWE3YTYtYzIyODQyNmY2MTJiIiwiY2xpZW50X2lkIjoidGVzdC1jbGllbnRfaWQiLCJ1c2VybmFtZSI6InRlc3QtdXNlcm5hbWUifQ.PY5_jWcdxhCyn2EpFpss7Q0R3_xH1PvHi4mxDLorpppHnciGT2kFLeutebi7XeLtTYwmttTxxg2tyUyX0_UF7zj_P-tdq-kZQlud1ENmRaUxLXO5mTFKXD7zPb6BPFNe0ewRQ7Uuv3lDk_IxOf-6i86VDYB8luyesEXq7ra4S4l8akFodW_QYBSZQnUva_CVyzsTNcmgGTyrz2NI6seT1x6Pt1uFdYI97FHKlCCWVL1Z042omfujfta8j8XkTWdhKf3dfsHRWjrw31xqOkgD7uwPKcrC0U-wIj3U0uX0Rz2Tk4T-kIq4XTkKttYpkJqOmMFAYuhk6MDjfRkPWBZhUA")
                              (mock/header "x-amzn-oidc-identity" "paakayttaja@solita.fi")
                              (mock/header "x-amzn-oidc-data" "eyJ0eXAiOiJKV1QiLCJraWQiOiJ0ZXN0LWtpZCIsImFsZyI6IlJTMjU2IiwiaXNzIjoidGVzdC1pc3MiLCJjbGllbnQiOiJ0ZXN0LWNsaWVudCIsInNpZ25lciI6InRlc3Qtc2lnbmVyIiwiZXhwIjoxODkzNDU2MDAwfQ.eyJzdWIiOiJwYWFrYXl0dGFqYUBzb2xpdGEuZmkiLCJjdXN0b206VklSVFVfbG9jYWxJRCI6InZ2aXJrYW1pZXMiLCJjdXN0b206VklSVFVfbG9jYWxPcmciOiJ0ZXN0aXZpcmFzdG8uZmkiLCJ1c2VybmFtZSI6InRlc3QtdXNlcm5hbWUiLCJleHAiOjE4OTM0NTYwMDAsImlzcyI6InRlc3QtaXNzIn0.BfuDVOFUReiJd6N05Re6affps_47AA0F5o-g6prmXgAnk4lB1S3k9RpovCFU3-R5Zn0p38QTiwi5dENHCHaj1A6MGHHKeYd7vBZK0VquuBxlIQH-4k1MWLvpYnkK3yuEvfmbRb3jYspCA_4N-AF21cCyjd15RiuIawLCEM0Km1DRgLhXIBta6XCGSRwaRmrT7boDRMp7hUkYPpoakCahMC70sjyuvLE0pjAy1_S09g4SkboentI7WhfsfN4uAHbKy6ViVMfsnwVVvKsM8dXav_a-6PoNGywuUbi8nHt8c20KiB_AzAEYSqxbRX1YBd0UHlYS16LbLtMBTOctCBLDMg")
                              (mock/header "Accept" "application/json")))
        response-body (j/read-value (:body response) j/keyword-keys-object-mapper)
        ]
    (t/is (:status response) 200)
    (t/is (= response-body {:valvoja-id                 kayttaja-id
                            :katuosoite                 "katu"
                            :ilmoitustunnus             nil
                            :rakennustunnus             "1035150826"
                            :postinumero                "65100"
                            :id                         1
                            :ilmoituspaikka-description "Netissä"
                            :ilmoituspaikka-id          2
                            :havaintopaiva              "2023-06-01"}))))

(def original-html->pdf pdf/html->pdf)

(def access-token
  "eyJraWQiOiJ0ZXN0LWtpZCIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJwYWFrYXl0dGFqYUBzb2xpdGEuZmkiLCJ0b2tlbl91c2UiOiJhY2Nlc3MiLCJzY29wZSI6Im9wZW5pZCIsImF1dGhfdGltZSI6MTU4MzIzMDk2OSwiaXNzIjoiaHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL3NvbGl0YS9ldHAtY29yZS9mZWF0dXJlL0FFLTQzLWF1dGgtaGVhZGVycy1oYW5kbGluZy9ldHAtYmFja2VuZC9zcmMvbWFpbi9yZXNvdXJjZXMiLCJleHAiOjE4OTM0NTYwMDAsImlhdCI6MTU4MzQxMzQyNCwidmVyc2lvbiI6MiwianRpIjoiNWZkZDdhMjktN2VlYS00ZjNkLWE3YTYtYzIyODQyNmY2MTJiIiwiY2xpZW50X2lkIjoidGVzdC1jbGllbnRfaWQiLCJ1c2VybmFtZSI6InRlc3QtdXNlcm5hbWUifQ.PY5_jWcdxhCyn2EpFpss7Q0R3_xH1PvHi4mxDLorpppHnciGT2kFLeutebi7XeLtTYwmttTxxg2tyUyX0_UF7zj_P-tdq-kZQlud1ENmRaUxLXO5mTFKXD7zPb6BPFNe0ewRQ7Uuv3lDk_IxOf-6i86VDYB8luyesEXq7ra4S4l8akFodW_QYBSZQnUva_CVyzsTNcmgGTyrz2NI6seT1x6Pt1uFdYI97FHKlCCWVL1Z042omfujfta8j8XkTWdhKf3dfsHRWjrw31xqOkgD7uwPKcrC0U-wIj3U0uX0Rz2Tk4T-kIq4XTkKttYpkJqOmMFAYuhk6MDjfRkPWBZhUA")

(def oidc-data "eyJ0eXAiOiJKV1QiLCJraWQiOiJ0ZXN0LWtpZCIsImFsZyI6IlJTMjU2IiwiaXNzIjoidGVzdC1pc3MiLCJjbGllbnQiOiJ0ZXN0LWNsaWVudCIsInNpZ25lciI6InRlc3Qtc2lnbmVyIiwiZXhwIjoxODkzNDU2MDAwfQ.eyJzdWIiOiJwYWFrYXl0dGFqYUBzb2xpdGEuZmkiLCJjdXN0b206VklSVFVfbG9jYWxJRCI6InZ2aXJrYW1pZXMiLCJjdXN0b206VklSVFVfbG9jYWxPcmciOiJ0ZXN0aXZpcmFzdG8uZmkiLCJ1c2VybmFtZSI6InRlc3QtdXNlcm5hbWUiLCJleHAiOjE4OTM0NTYwMDAsImlzcyI6InRlc3QtaXNzIn0.BfuDVOFUReiJd6N05Re6affps_47AA0F5o-g6prmXgAnk4lB1S3k9RpovCFU3-R5Zn0p38QTiwi5dENHCHaj1A6MGHHKeYd7vBZK0VquuBxlIQH-4k1MWLvpYnkK3yuEvfmbRb3jYspCA_4N-AF21cCyjd15RiuIawLCEM0Km1DRgLhXIBta6XCGSRwaRmrT7boDRMp7hUkYPpoakCahMC70sjyuvLE0pjAy1_S09g4SkboentI7WhfsfN4uAHbKy6ViVMfsnwVVvKsM8dXav_a-6PoNGywuUbi8nHt8c20KiB_AzAEYSqxbRX1YBd0UHlYS16LbLtMBTOctCBLDMg")

(t/deftest kaytonvalvonta-kuulemiskirje-test
  (t/testing "Käytönvalvonta / Kuulemiskirje toimenpide is created successfully and document is generated with correct information"
    (first (test-kayttajat/insert!
             [{:etunimi  "Asian",
               :sukunimi "Tuntija",
               :email    "testi@ara.fi",
               :puhelin  "0504363675457",
               :rooli    2,
               :virtu    {:localid "vvirkamies"
                          :organisaatio "testivirasto.fi"}}]))
    ;; Add the valvonta and previous toimenpides
    ;; so that käytönvalvonta / kuulemiskirje toimenpide can be created
    (let [valvonta-id (valvonta-service/add-valvonta! ts/*db* {:katuosoite        "Testitie 5"
                                                               :postinumero       "90100"
                                                               :ilmoituspaikka-id 0})
          kehotus-timestamp (-> (LocalDate/of 2023 6 12)
                                (.atStartOfDay (ZoneId/systemDefault))
                                .toInstant)
          varoitus-timestamp (-> (LocalDate/of 2023 7 13)
                                 (.atStartOfDay (ZoneId/systemDefault))
                                 .toInstant)
          html->pdf-called? (atom false)]

      ;; Add osapuoli to the valvonta
      (valvonta-service/add-henkilo! ts/*db*
                                     valvonta-id
                                     {:toimitustapa-description nil
                                      :toimitustapa-id          0
                                      :email                    nil
                                      :rooli-id                 0
                                      :jakeluosoite             "Testikatu 12"
                                      :postitoimipaikka         "Helsinki"
                                      :puhelin                  nil
                                      :sukunimi                 "Talonomistaja"
                                      :postinumero              "00100"
                                      :henkilotunnus            "000000-0000"
                                      :rooli-description        ""
                                      :etunimi                  "Testi"
                                      :vastaanottajan-tarkenne  nil
                                      :maa                      "FI"})

      ;; Add kehotus-toimenpide to the valvonta
      (jdbc/insert! ts/*db* :vk_toimenpide {:valvonta_id   valvonta-id
                                            :type_id       2
                                            :create_time   kehotus-timestamp
                                            :publish_time  kehotus-timestamp
                                            :deadline_date (LocalDate/of 2023 7 12)})
      ;; Add varoitus-toimenpide to the valvonta
      (jdbc/insert! ts/*db* :vk_toimenpide {:valvonta_id   valvonta-id
                                            :type_id       3
                                            :create_time   varoitus-timestamp
                                            :publish_time  varoitus-timestamp
                                            :deadline_date (LocalDate/of 2023 8 13)})

      (with-redefs [pdf/html->pdf (fn [html-doc output-stream]
                                    ;; Mocking the pdf rendering function so that the document contents can be asserted
                                    ;; Compare the created document to the snapshot
                                    (t/is (= html-doc
                                             (slurp (io/resource "documents/kaskypaatoskuulemiskirje.html"))))
                                    (reset! html->pdf-called? true)
                                    ;;Calling original implementation to ensure the functionality doesn't change
                                    (original-html->pdf html-doc output-stream))]
        (let [new-toimenpide {:type-id       7
                              :deadline-date (.toString (LocalDate/of 2023 7 22))
                              :template-id   5
                              :description   "Lähetetään kuulemiskirje, kun myyjä ei ole hankkinut energiatodistusta eikä vastannut kehotukseen tai varoitukseen"
                              :fine          800}
              response (handler (-> (mock/request :post (format "/api/private/valvonta/kaytto/%s/toimenpiteet" valvonta-id))
                                    (mock/json-body new-toimenpide)
                                    (mock/header "x-amzn-oidc-accesstoken" access-token)
                                    (mock/header "x-amzn-oidc-identity" "paakayttaja@solita.fi")
                                    (mock/header "x-amzn-oidc-data" oidc-data)
                                    (mock/header "Accept" "application/json")
                                    ))]
          (t/is (true? @html->pdf-called?))
          (t/is (= (:status response) 201)))))))
