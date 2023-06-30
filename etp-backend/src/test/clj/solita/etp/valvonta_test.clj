(ns solita.etp.valvonta-test
  (:require
    [clojure.java.io :as io]
    [clojure.java.jdbc :as jdbc]
    [clojure.test :as t]
    [jsonista.core :as j]
    [ring.mock.request :as mock]
    [solita.common.time :as time]
    [solita.etp.handler :as handler]
    [solita.etp.schema.valvonta-kaytto :as valvonta-schema]
    [solita.etp.service.pdf :as pdf]
    [solita.etp.service.suomifi-viestit :as suomifi-viestit]
    [solita.etp.service.valvonta-kaytto :as valvonta-service]
    [solita.etp.test-data.generators :as generators]
    [solita.etp.test-data.kayttaja :as test-kayttajat]
    [solita.etp.test-system :as ts])
  (:import (java.time Clock LocalDate ZoneId)))

(t/use-fixtures :each ts/fixture)

(defn- handler [req]
  ; Mimics real handler usage with test assets
  (handler/handler (merge req {:db ts/*db* :aws-s3-client ts/*aws-s3-client*})))

(defn- non-nil-key->string [m k]
  (if (some? (k m))
    (assoc m k (str (k m))) m))

(def access-token
  "eyJraWQiOiJ0ZXN0LWtpZCIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJwYWFrYXl0dGFqYUBzb2xpdGEuZmkiLCJ0b2tlbl91c2UiOiJhY2Nlc3MiLCJzY29wZSI6Im9wZW5pZCIsImF1dGhfdGltZSI6MTU4MzIzMDk2OSwiaXNzIjoiaHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL3NvbGl0YS9ldHAtY29yZS9mZWF0dXJlL0FFLTQzLWF1dGgtaGVhZGVycy1oYW5kbGluZy9ldHAtYmFja2VuZC9zcmMvbWFpbi9yZXNvdXJjZXMiLCJleHAiOjE4OTM0NTYwMDAsImlhdCI6MTU4MzQxMzQyNCwidmVyc2lvbiI6MiwianRpIjoiNWZkZDdhMjktN2VlYS00ZjNkLWE3YTYtYzIyODQyNmY2MTJiIiwiY2xpZW50X2lkIjoidGVzdC1jbGllbnRfaWQiLCJ1c2VybmFtZSI6InRlc3QtdXNlcm5hbWUifQ.PY5_jWcdxhCyn2EpFpss7Q0R3_xH1PvHi4mxDLorpppHnciGT2kFLeutebi7XeLtTYwmttTxxg2tyUyX0_UF7zj_P-tdq-kZQlud1ENmRaUxLXO5mTFKXD7zPb6BPFNe0ewRQ7Uuv3lDk_IxOf-6i86VDYB8luyesEXq7ra4S4l8akFodW_QYBSZQnUva_CVyzsTNcmgGTyrz2NI6seT1x6Pt1uFdYI97FHKlCCWVL1Z042omfujfta8j8XkTWdhKf3dfsHRWjrw31xqOkgD7uwPKcrC0U-wIj3U0uX0Rz2Tk4T-kIq4XTkKttYpkJqOmMFAYuhk6MDjfRkPWBZhUA")

(def oidc-data "eyJ0eXAiOiJKV1QiLCJraWQiOiJ0ZXN0LWtpZCIsImFsZyI6IlJTMjU2IiwiaXNzIjoidGVzdC1pc3MiLCJjbGllbnQiOiJ0ZXN0LWNsaWVudCIsInNpZ25lciI6InRlc3Qtc2lnbmVyIiwiZXhwIjoxODkzNDU2MDAwfQ.eyJzdWIiOiJwYWFrYXl0dGFqYUBzb2xpdGEuZmkiLCJjdXN0b206VklSVFVfbG9jYWxJRCI6InZ2aXJrYW1pZXMiLCJjdXN0b206VklSVFVfbG9jYWxPcmciOiJ0ZXN0aXZpcmFzdG8uZmkiLCJ1c2VybmFtZSI6InRlc3QtdXNlcm5hbWUiLCJleHAiOjE4OTM0NTYwMDAsImlzcyI6InRlc3QtaXNzIn0.BfuDVOFUReiJd6N05Re6affps_47AA0F5o-g6prmXgAnk4lB1S3k9RpovCFU3-R5Zn0p38QTiwi5dENHCHaj1A6MGHHKeYd7vBZK0VquuBxlIQH-4k1MWLvpYnkK3yuEvfmbRb3jYspCA_4N-AF21cCyjd15RiuIawLCEM0Km1DRgLhXIBta6XCGSRwaRmrT7boDRMp7hUkYPpoakCahMC70sjyuvLE0pjAy1_S09g4SkboentI7WhfsfN4uAHbKy6ViVMfsnwVVvKsM8dXav_a-6PoNGywuUbi8nHt8c20KiB_AzAEYSqxbRX1YBd0UHlYS16LbLtMBTOctCBLDMg")


(defn- with-virtu-user [request]
  (-> request
      (mock/header "x-amzn-oidc-accesstoken" access-token)
      (mock/header "x-amzn-oidc-identity" "paakayttaja@solita.fi")
      (mock/header "x-amzn-oidc-data" oidc-data)))

(defn- add-valvonta-and-map-id! [valvonta]
  (assoc valvonta :id (valvonta-service/add-valvonta! ts/*db* valvonta)))

(defn- add-toimenpide-and-map-id! [valvonta-id toimenpide]
  (merge toimenpide (valvonta-service/add-toimenpide! ts/*db* ts/*aws-s3-client* {} valvonta-id toimenpide)))

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
      (with-redefs [suomifi-viestit/send-message! assert-suomifi-message-sent]
        (let [kayttaja-id (test-kayttajat/insert-virtu-paakayttaja!)
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
                    :deadline-date (str (LocalDate/of 2024 11 10))
                    :template-id   2
                    :description   "Tee jotain"
                    }
              response (handler (-> (mock/request :post (format "/api/private/valvonta/kaytto/%s/toimenpiteet" valvonta-id))
                                    (mock/json-body body)
                                    (with-virtu-user)
                                    (mock/header "Accept" "application/json")))
              response-body (j/read-value (:body response) j/keyword-keys-object-mapper)]

          (t/is (= (:status response) 201))
          (t/is (= response-body {:id 1}))
          (t/is (deref suomifi-message-sent 5000 false))
          )))))

(t/deftest fetching-valvonta
  (t/testing "Yksittäisen valvonnan hakeminen palauttaa vastauksen")
  (let [
        kayttaja-id (test-kayttajat/insert-virtu-paakayttaja!)
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
                              (with-virtu-user)
                              (mock/header "Accept" "application/json")))
        response-body (j/read-value (:body response) j/keyword-keys-object-mapper)
        ]
    (t/is (= (:status response) 200))
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

(t/deftest kaytonvalvonta-kuulemiskirje-test
  (t/testing "Käytönvalvonta / Kuulemiskirje toimenpide is created successfully and document is generated with correct information"
    (first (test-kayttajat/insert!
             [{:etunimi  "Asian"
               :sukunimi "Tuntija"
               :email    "testi@ara.fi"
               :puhelin  "0504363675457"
               :rooli    2
               :virtu    {:localid      "vvirkamies"
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

      ;; Mock the current time to ensure that the document has a fixed date
      (with-bindings {#'time/clock    (Clock/fixed (-> (LocalDate/of 2023 6 26)
                                                       (.atStartOfDay time/timezone)
                                                       .toInstant)
                                                   time/timezone)
                      #'pdf/html->pdf (fn [html-doc output-stream]
                                        ;; Mocking the pdf rendering function so that the document contents can be asserted
                                        ;; Compare the created document to the snapshot
                                        (t/is (= html-doc
                                                 (slurp (io/resource "documents/kaskypaatoskuulemiskirje.html"))))
                                        (reset! html->pdf-called? true)
                                        ;;Calling original implementation to ensure the functionality doesn't change
                                        (original-html->pdf html-doc output-stream))}
        (let [new-toimenpide {:type-id       7
                              :deadline-date (str (LocalDate/of 2023 7 22))
                              :template-id   5
                              :description   "Lähetetään kuulemiskirje, kun myyjä ei ole hankkinut energiatodistusta eikä vastannut kehotukseen tai varoitukseen"
                              :fine          800}
              response (handler (-> (mock/request :post (format "/api/private/valvonta/kaytto/%s/toimenpiteet" valvonta-id))
                                    (mock/json-body new-toimenpide)
                                    (with-virtu-user)
                                    (mock/header "Accept" "application/json")))]
          (t/is (true? @html->pdf-called?))
          (t/is (= (:status response) 201)))))))

(t/deftest adding-and-fetching-valvonta
  (let [kayttaja-id (test-kayttajat/insert-virtu-paakayttaja!)
        valvonta (-> {}
                     (generators/complete valvonta-schema/ValvontaSave)
                     (non-nil-key->string :havaintopaiva) ; Jackson has problems encoding local date
                     (assoc :ilmoituspaikka-id 1)
                     (assoc :valvoja-id kayttaja-id))]
    (t/testing "Uuden valvonnan luominen"
      (let [response (handler (-> (mock/request :post "/api/private/valvonta/kaytto")
                                  (mock/json-body valvonta)
                                  (with-virtu-user)
                                  (mock/header "Accept" "application/json")))
            response-body (j/read-value (:body response) j/keyword-keys-object-mapper)]
        (t/is (= (:status response) 201))
        (t/is (= response-body {:id 1}))))
    (t/testing "Luotu valvonta on tallennettu ja voidaan hakea"
      (let [fetch-response (handler (-> (mock/request :get (format "/api/private/valvonta/kaytto/1"))
                                        (with-virtu-user)
                                        (mock/header "Accept" "application/json")))
            fetched-valvonta (j/read-value (:body fetch-response) j/keyword-keys-object-mapper)
            expected-valvonta (assoc valvonta :id 1)]
        (t/is (= (:status fetch-response) 200))
        (t/is (= fetched-valvonta expected-valvonta))))))

(t/deftest get-toimenpiteet-for-valvonta
  (let [kayttaja-id (test-kayttajat/insert-virtu-paakayttaja!)
        ;; Create two valvontas
        valvonnat (repeatedly 2 #(-> {}
                                     (generators/complete valvonta-schema/ValvontaSave)
                                     (assoc :ilmoituspaikka-id 1
                                            :valvoja-id kayttaja-id)
                                     add-valvonta-and-map-id!))
        ;; Create 6 toimenpide for each valvonta
        toimenpiteet (->> (repeatedly 12 (fn [] (generators/complete {} valvonta-schema/ToimenpideAdd)))
                          (map vector (flatten (repeatedly (constantly [1 2]))))
                          (map (fn [[template-id toimenpide]] (assoc toimenpide :template-id template-id)))
                          (map (fn [toimenpide] (assoc toimenpide :type-id 1)))
                          (map vector (flatten (repeat (map :id valvonnat)))))]
    (doseq [[valvonta-id toimenpide] toimenpiteet]
      (add-toimenpide-and-map-id! valvonta-id toimenpide))
    (t/testing "Valvonnalle palautetaan 6 toimenpidettä"
      (let [response (handler (-> (mock/request :get (format "/api/private/valvonta/kaytto/%s/toimenpiteet" (-> valvonnat first :id)))
                                  (with-virtu-user)
                                  (mock/header "Accept" "application/json")))
            response-body (j/read-value (:body response) j/keyword-keys-object-mapper)]
        (t/is (= (:status response) 200))
        (t/is (= (count response-body) 6))))))


(t/deftest get-valvonnat-with-filters
  (let [kayttaja-id (test-kayttajat/insert-virtu-paakayttaja!)
        ;; Create two valvontas
        valvonnat (repeatedly 2 #(-> {}
                                     (generators/complete valvonta-schema/ValvontaSave)
                                     (assoc :ilmoituspaikka-id 1
                                            :valvoja-id kayttaja-id)
                                     add-valvonta-and-map-id!))
        ;; Create a toimenpide for each valvonta
        toimenpiteet (->> (repeatedly 2 (fn [] (generators/complete {} valvonta-schema/ToimenpideAdd)))
                          (map (fn [toimenpide] (assoc toimenpide :type-id 1)))
                          (map vector (flatten (repeatedly (constantly [1 2])))) ; Use two different templates for every other toimenpide
                          (map (fn [[template-id toimenpide]] (assoc toimenpide :template-id template-id)))
                          (map vector (flatten (repeat (map :id valvonnat))))
                          (mapv #(apply add-toimenpide-and-map-id! %)))]
    (t/testing "Hae valvontojen määrä joissa on käytetty asiakirjapohjaa 1"
      (let [response (handler (-> (mock/request :get "/api/private/valvonta/kaytto/count")
                                  (mock/query-string {:asiakirjapohja-id 1})
                                  (with-virtu-user)
                                  (mock/header "Accept" "application/json")))
            response-body (j/read-value (:body response) j/keyword-keys-object-mapper)]
        (t/is (= (:status response) 200))
        (t/is (= response-body {:count 1}))))
    (t/testing "Hae valvonnat joissa on käytetty asiakirjapohjaa 1"
      (let [response (handler (-> (mock/request :get "/api/private/valvonta/kaytto")
                                  (mock/query-string {:asiakirjapohja-id 1})
                                  (with-virtu-user)
                                  (mock/header "Accept" "application/json")))
            response-body (j/read-value (:body response) j/keyword-keys-object-mapper)
            expected-valvonta (-> (first valvonnat)
                                  (assoc :henkilot []       ;Add synthetic fields
                                         :yritykset []
                                         :energiatodistus nil)
                                  (non-nil-key->string :havaintopaiva))
            expected-last-toimenpide (-> (first toimenpiteet)
                                         (dissoc :description) ; This is not sent here
                                         (assoc :diaarinumero nil) ; Would be generated by asha
                                         (non-nil-key->string :deadline-date))
            received-valvonta (first response-body)
            received-last-toimenpide (-> (:last-toimenpide received-valvonta)
                                         (dissoc :publish-time :create-time))]

        (t/is (= (:status response) 200))
        (t/is (= (count response-body) 1))
        (t/is (= (dissoc received-valvonta :last-toimenpide) expected-valvonta))
        (t/is (= received-last-toimenpide expected-last-toimenpide))))))
