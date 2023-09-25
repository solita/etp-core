(ns solita.etp.valvonta-test
  (:require
    [clojure.java.io :as io]
    [clojure.java.jdbc :as jdbc]
    [clojure.test :as t]
    [jsonista.core :as j]
    [ring.mock.request :as mock]
    [solita.common.time :as time]
    [solita.etp.schema.valvonta-kaytto :as valvonta-schema]
    [solita.etp.service.pdf :as pdf]
    [solita.etp.service.suomifi-viestit :as suomifi-viestit]
    [solita.etp.service.valvonta-kaytto :as valvonta-service]
    [solita.etp.test-data.generators :as generators]
    [solita.etp.test-data.kayttaja :as test-kayttajat]
    [solita.etp.test-system :as ts])
  (:import (java.time Clock LocalDate ZoneId)))

(t/use-fixtures :each ts/fixture)

(defn- non-nil-key->string [m k]
  (if (some? (k m))
    (assoc m k (str (k m))) m))

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
              response (ts/handler (-> (mock/request :post (format "/api/private/valvonta/kaytto/%s/toimenpiteet" valvonta-id))
                                       (mock/json-body body)
                                       (test-kayttajat/with-virtu-user)
                                       (mock/header "Accept" "application/json")))
              response-body (j/read-value (:body response) j/keyword-keys-object-mapper)]

          (t/is (= (:status response) 201))
          (t/is (= response-body {:id 1}))
          (t/is (deref suomifi-message-sent 5000 false))
          )))))

(t/deftest fetching-valvonta
  (let [kayttaja-id (test-kayttajat/insert-virtu-paakayttaja!)
        valvonta-id (valvonta-service/add-valvonta!
                      ts/*db*
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
                                  })))]
    (t/testing "Yksittäisen valvonnan hakeminen palauttaa vastauksen"
      (let [response (ts/handler (-> (mock/request :get (format "/api/private/valvonta/kaytto/%s" valvonta-id))
                                     (test-kayttajat/with-virtu-user)
                                     (mock/header "Accept" "application/json")))
            response-body (j/read-value (:body response) j/keyword-keys-object-mapper)]
        (t/is (= (:status response) 200))
        (t/is (= response-body {:valvoja-id                 kayttaja-id
                                :katuosoite                 "katu"
                                :ilmoitustunnus             nil
                                :rakennustunnus             "1035150826"
                                :postinumero                "65100"
                                :id                         1
                                :ilmoituspaikka-description "Netissä"
                                :ilmoituspaikka-id          2
                                :havaintopaiva              "2023-06-01"}))))))

(t/deftest fetching-johtaja-test
  (let [kayttaja-id (test-kayttajat/insert-virtu-paakayttaja!)]
    (t/testing "Kun käskypäätös / varsinainen päätös - toimenpidettä ei ole, osaston päällikön tietoja ei löydy"
      (let [response (ts/handler (-> (mock/request :get "/api/private/valvonta/kaytto/johtaja")
                                     (test-kayttajat/with-virtu-user)
                                     (mock/header "Accept" "application/json")))
            response-body (j/read-value (:body response) j/keyword-keys-object-mapper)]
        (t/is (= (:status response) 200))
        (t/is (= response-body {:department-head-name     nil
                                :department-head-title-fi nil
                                :department-head-title-sv nil}))))

    (t/testing "käskypäätös / varsinainen päätös -toimenpiteen olemassaollessa osaston päällikön tiedot täydentyvät responseen"
      (let [valvonta-id (valvonta-service/add-valvonta!
                          ts/*db*
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
                                      })))]
        ;; Add käskypäätös / varsinainen päätös toimenpide so department-head-name
        ;; and department-head-title are populated in the response
        (jdbc/insert! ts/*db*
                      :vk_toimenpide
                      {:valvonta_id        valvonta-id
                       :type_id            8
                       :create_time        (-> (LocalDate/of 2023 8 10)
                                               (.atStartOfDay (ZoneId/systemDefault))
                                               .toInstant)
                       :publish_time       (-> (LocalDate/of 2023 8 10)
                                               (.atStartOfDay (ZoneId/systemDefault))
                                               .toInstant)
                       :deadline_date      (LocalDate/of 2023 8 28)
                       :diaarinumero       "ARA-05.03.01-2023-235"
                       :type_specific_data {:fine                     6100
                                            :department-head-name     "Testi Testinen"
                                            :department-head-title-fi "Ylitarkastaja"
                                            :department-head-title-sv "Ylitarkastaja på svenska"}})
        (let [response (ts/handler (-> (mock/request :get "/api/private/valvonta/kaytto/johtaja")
                                       (test-kayttajat/with-virtu-user)
                                       (mock/header "Accept" "application/json")))
              response-body (j/read-value (:body response) j/keyword-keys-object-mapper)]
          (t/is (= (:status response) 200))
          (t/is (= response-body {:department-head-name     "Testi Testinen"
                                  :department-head-title-fi "Ylitarkastaja"
                                  :department-head-title-sv "Ylitarkastaja på svenska"})))))))

(def original-html->pdf pdf/html->pdf)

(defn html->pdf-with-assertion [doc-path-to-compare-to html->pdf-called? html-doc output-stream]
  ;; Mocking the pdf rendering function so that the document contents can be asserted
  ;; Compare the created document to the snapshot
  (t/is (= html-doc
           (slurp (io/resource doc-path-to-compare-to))))
  (reset! html->pdf-called? true)
  ;;Calling original implementation to ensure the functionality doesn't change
  (original-html->pdf html-doc output-stream))

(t/deftest kaskypaatos-kuulemiskirje-test
  ;; Add the main user for the following tests
  (test-kayttajat/insert-virtu-paakayttaja!
    {:etunimi  "Asian"
     :sukunimi "Tuntija"
     :email    "testi@ara.fi"
     :puhelin  "0504363675457"})
  (t/testing "Käskypäätös / Kuulemiskirje toimenpide is created successfully for yksityishenkilö and document is generated with correct information"
    ;; Add the valvonta and previous toimenpides
    ;; so that käskypäätös / kuulemiskirje toimenpide can be created
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
                      #'pdf/html->pdf (partial html->pdf-with-assertion
                                               "documents/kaskypaatoskuulemiskirje-yksityishenkilo.html"
                                               html->pdf-called?)}
        (let [new-toimenpide {:type-id            7
                              :deadline-date      (str (LocalDate/of 2023 7 22))
                              :template-id        5
                              :description        "Lähetetään kuulemiskirje, kun myyjä ei ole hankkinut energiatodistusta eikä vastannut kehotukseen tai varoitukseen"
                              :type-specific-data {:fine 800}}
              response (ts/handler (-> (mock/request :post (format "/api/private/valvonta/kaytto/%s/toimenpiteet" valvonta-id))
                                       (mock/json-body new-toimenpide)
                                       (test-kayttajat/with-virtu-user)
                                       (mock/header "Accept" "application/json")))]
          (t/is (true? @html->pdf-called?))
          (t/is (= (:status response) 201))))))

  (t/testing "Käskypäätös / Kuulemiskirje toimenpide is created successfully for yritys and document is generated with correct information"
    ;; Add the valvonta and previous toimenpides
    ;; so that käskypäätös / kuulemiskirje toimenpide can be created
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
      (valvonta-service/add-yritys! ts/*db*
                                    valvonta-id
                                    {:nimi                     "Yritysomistaja"
                                     :toimitustapa-description nil
                                     :toimitustapa-id          0
                                     :email                    nil
                                     :rooli-id                 0
                                     :jakeluosoite             "Testikatu 12"
                                     :vastaanottajan-tarkenne  "Lisäselite C/O"
                                     :postitoimipaikka         "Helsinki"
                                     :puhelin                  nil
                                     :postinumero              "00100"
                                     :rooli-description        ""
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
                      #'pdf/html->pdf (partial html->pdf-with-assertion
                                               "documents/kaskypaatoskuulemiskirje-yritys.html"
                                               html->pdf-called?)}
        (let [new-toimenpide {:type-id            7
                              :deadline-date      (str (LocalDate/of 2023 7 22))
                              :template-id        5
                              :description        "Lähetetään kuulemiskirje, kun myyjä ei ole hankkinut energiatodistusta eikä vastannut kehotukseen tai varoitukseen"
                              :type-specific-data {:fine 9000}}
              response (ts/handler (-> (mock/request :post (format "/api/private/valvonta/kaytto/%s/toimenpiteet" valvonta-id))
                                       (mock/json-body new-toimenpide)
                                       (test-kayttajat/with-virtu-user)
                                       (mock/header "Accept" "application/json")))]
          (t/is (true? @html->pdf-called?))
          (t/is (= (:status response) 201)))))))

(t/deftest kaskypaatos-varsinainen-paatos-test
  ;; Add the main user for the following tests
  (test-kayttajat/insert-virtu-paakayttaja!
    {:etunimi  "Asian"
     :sukunimi "Tuntija"
     :email    "testi@ara.fi"
     :puhelin  "0504363675457"})
  (t/testing "Käskypäätös / varsinainen päätös toimenpide is created successfully for yksityishenkilö and document is generated with correct information"
    ;; Add the valvonta and previous toimenpides
    ;; so that käskypäätös / kuulemiskirje toimenpide can be created
    (let [valvonta-id (valvonta-service/add-valvonta! ts/*db* {:katuosoite        "Testitie 5"
                                                               :postinumero       "90100"
                                                               :ilmoituspaikka-id 0})
          kehotus-timestamp (-> (LocalDate/of 2023 6 12)
                                (.atStartOfDay (ZoneId/systemDefault))
                                .toInstant)
          varoitus-timestamp (-> (LocalDate/of 2023 7 13)
                                 (.atStartOfDay (ZoneId/systemDefault))
                                 .toInstant)
          kuulemiskirje-timestamp (-> (LocalDate/of 2023 7 13)
                                      (.atStartOfDay (ZoneId/systemDefault))
                                      .toInstant)
          html->pdf-called? (atom false)
          ;; Add osapuoli to the valvonta
          osapuoli-id (valvonta-service/add-henkilo!
                        ts/*db*
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
                         :maa                      "FI"})]
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

      ;; Add käskypäätös / kuulemiskirje toimenpide to the valvonta
      (jdbc/insert! ts/*db* :vk_toimenpide {:valvonta_id        valvonta-id
                                            :type_id            7
                                            :create_time        kuulemiskirje-timestamp
                                            :publish_time       kuulemiskirje-timestamp
                                            :deadline_date      (LocalDate/of 2023 8 27)
                                            :type_specific_data {:fine 9000}
                                            :diaarinumero       "ARA-05.03.01-2023-159"})
      ;; Mock the current time to ensure that the document has a fixed date
      (with-bindings {#'time/clock    (Clock/fixed (-> (LocalDate/of 2023 8 28)
                                                       (.atStartOfDay time/timezone)
                                                       .toInstant)
                                                   time/timezone)
                      #'pdf/html->pdf (partial html->pdf-with-assertion
                                               "documents/kaskypaatos-varsinainen-paatos-yksityishenkilo.html"
                                               html->pdf-called?)}
        (let [new-toimenpide {:type-id            8
                              :deadline-date      (str (LocalDate/of 2023 10 4))
                              :template-id        6
                              :description        "Tehdään varsinainen päätös, omistaja vastasi kuulemiskirjeeseen"
                              :type-specific-data {:fine                     857
                                                   :recipient-answered       true
                                                   :answer-commentary-fi     "En tiennyt, että todistus tarvitaan :("
                                                   :answer-commentary-sv     "Jag visste inte att ett intyg behövs :("
                                                   :statement-fi             "Tämän kerran annetaan anteeksi, kun hän ei tiennyt."
                                                   :statement-sv             "Han vet inte. Vi förlotar."
                                                   :osapuoli-specific-data   [{:osapuoli-id        osapuoli-id
                                                                               :hallinto-oikeus-id 1
                                                                               :document           true}]
                                                   :department-head-title-fi "Apulaisjohtaja"
                                                   :department-head-title-sv "Apulaisjohtaja på svenska"
                                                   :department-head-name     "Yli Päällikkö"}}
              response (ts/handler (-> (mock/request :post (format "/api/private/valvonta/kaytto/%s/toimenpiteet" valvonta-id))
                                       (mock/json-body new-toimenpide)
                                       (test-kayttajat/with-virtu-user)
                                       (mock/header "Accept" "application/json")))]
          (t/is (true? @html->pdf-called?))
          (t/is (= (:status response) 201))))))

  (t/testing "Käskypäätös / varsinainen päätös toimenpide is created successfully for yritys and document is generated with correct information"
    ;; Add the valvonta and previous toimenpides
    ;; so that käskypäätös / kuulemiskirje toimenpide can be created
    (let [valvonta-id (valvonta-service/add-valvonta! ts/*db* {:katuosoite        "Testitie 5"
                                                               :postinumero       "90100"
                                                               :ilmoituspaikka-id 0})
          kehotus-timestamp (-> (LocalDate/of 2023 6 12)
                                (.atStartOfDay (ZoneId/systemDefault))
                                .toInstant)
          varoitus-timestamp (-> (LocalDate/of 2023 7 13)
                                 (.atStartOfDay (ZoneId/systemDefault))
                                 .toInstant)
          kuulemiskirje-timestamp (-> (LocalDate/of 2023 7 13)
                                      (.atStartOfDay (ZoneId/systemDefault))
                                      .toInstant)
          html->pdf-called? (atom false)
          ;; Add osapuoli to the valvonta
          osapuoli-id (valvonta-service/add-yritys!
                        ts/*db*
                        valvonta-id
                        {:nimi                     "Yritysomistaja"
                         :toimitustapa-description nil
                         :toimitustapa-id          0
                         :email                    nil
                         :rooli-id                 0
                         :jakeluosoite             "Testikatu 12"
                         :vastaanottajan-tarkenne  "Lisäselite C/O"
                         :postitoimipaikka         "Helsinki"
                         :puhelin                  nil
                         :postinumero              "00100"
                         :rooli-description        ""
                         :maa                      "FI"})]
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

      ;; Add käskypäätös / kuulemiskirje toimenpide to the valvonta
      (jdbc/insert! ts/*db* :vk_toimenpide {:valvonta_id        valvonta-id
                                            :type_id            7
                                            :create_time        kuulemiskirje-timestamp
                                            :publish_time       kuulemiskirje-timestamp
                                            :deadline_date      (LocalDate/of 2023 8 27)
                                            :diaarinumero       "ARA-05.03.01-2023-132"
                                            :type_specific_data {:fine 9000}})
      ;; Mock the current time to ensure that the document has a fixed date
      (with-bindings {#'time/clock    (Clock/fixed (-> (LocalDate/of 2023 8 28)
                                                       (.atStartOfDay time/timezone)
                                                       .toInstant)
                                                   time/timezone)
                      #'pdf/html->pdf (partial html->pdf-with-assertion
                                               "documents/kaskypaatos-varsinainen-paatos-yritys.html"
                                               html->pdf-called?)}
        (let [new-toimenpide {:type-id            8
                              :deadline-date      (str (LocalDate/of 2023 10 4))
                              :template-id        6
                              :description        "Tehdään varsinainen päätös, omistaja vastasi kuulemiskirjeeseen"
                              :type-specific-data {:fine                     857
                                                   :recipient-answered       false
                                                   :answer-commentary-fi     "Yritys ei ollut tavoitettavissa ollenkaan asian tiimoilta."
                                                   :answer-commentary-sv     "Företaget var inte nåbar alls angående ärendet."
                                                   :statement-fi             "Yritys tuomitaan sakkoihin."
                                                   :statement-sv             "Företaget döms till böter."
                                                   :osapuoli-specific-data   [{:osapuoli-id        osapuoli-id
                                                                               :hallinto-oikeus-id 2
                                                                               :document           true}]
                                                   :department-head-title-fi "Senior Vice President"
                                                   :department-head-title-sv "Kungen"
                                                   :department-head-name     "Jane Doe"}}
              response (ts/handler (-> (mock/request :post (format "/api/private/valvonta/kaytto/%s/toimenpiteet" valvonta-id))
                                       (mock/json-body new-toimenpide)
                                       (test-kayttajat/with-virtu-user)
                                       (mock/header "Accept" "application/json")))]
          (t/is (true? @html->pdf-called?))
          (t/is (= (:status response) 201))))))

  (t/testing "Käskypäätös / varsinainen päätös toimenpide is created successfully when there are multiple osapuolis but one lives abroad and will not receive the document because of being outside court jurisdiction"
    ;; Add the valvonta and previous toimenpides
    ;; so that käskypäätös / kuulemiskirje toimenpide can be created
    (let [valvonta-id (valvonta-service/add-valvonta! ts/*db* {:katuosoite        "Testitie 5"
                                                               :postinumero       "90100"
                                                               :ilmoituspaikka-id 0})
          kehotus-timestamp (-> (LocalDate/of 2023 6 12)
                                (.atStartOfDay (ZoneId/systemDefault))
                                .toInstant)
          varoitus-timestamp (-> (LocalDate/of 2023 7 13)
                                 (.atStartOfDay (ZoneId/systemDefault))
                                 .toInstant)
          kuulemiskirje-timestamp (-> (LocalDate/of 2023 7 13)
                                      (.atStartOfDay (ZoneId/systemDefault))
                                      .toInstant)
          html->pdf-called? (atom false)
          ;; Add osapuolis to the valvonta
          osapuoli-id (valvonta-service/add-henkilo!
                        ts/*db*
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
          osapuoli-id-2 (valvonta-service/add-henkilo!
                          ts/*db*
                          valvonta-id
                          {:toimitustapa-description nil
                           :toimitustapa-id          0
                           :email                    nil
                           :rooli-id                 0
                           :jakeluosoite             "Testikatu 13"
                           :postitoimipaikka         "Stockholm"
                           :puhelin                  nil
                           :sukunimi                 "Omistaja"
                           :postinumero              "00000"
                           :henkilotunnus            "000000-0001"
                           :rooli-description        ""
                           :etunimi                  "Toinen"
                           :vastaanottajan-tarkenne  nil
                           :maa                      "SV"})]
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

      ;; Add käskypäätös / kuulemiskirje toimenpide to the valvonta
      (jdbc/insert! ts/*db* :vk_toimenpide {:valvonta_id        valvonta-id
                                            :type_id            7
                                            :create_time        kuulemiskirje-timestamp
                                            :publish_time       kuulemiskirje-timestamp
                                            :deadline_date      (LocalDate/of 2023 8 27)
                                            :type_specific_data {:fine 9000}
                                            :diaarinumero       "ARA-05.03.01-2023-159"})
      ;; Mock the current time to ensure that the document has a fixed date
      (with-bindings {#'time/clock    (Clock/fixed (-> (LocalDate/of 2023 8 28)
                                                       (.atStartOfDay time/timezone)
                                                       .toInstant)
                                                   time/timezone)
                      ;; Assert that the created document is for the correct osapuoli
                      #'pdf/html->pdf (partial html->pdf-with-assertion
                                               "documents/kaskypaatos-varsinainen-paatos-yksityishenkilo.html"
                                               html->pdf-called?)}
        (let [new-toimenpide {:type-id            8
                              :deadline-date      (str (LocalDate/of 2023 10 4))
                              :template-id        6
                              :description        "Tehdään varsinainen päätös, omistaja vastasi kuulemiskirjeeseen"
                              :type-specific-data {:fine                     857
                                                   :recipient-answered       true
                                                   :answer-commentary-fi     "En tiennyt, että todistus tarvitaan :("
                                                   :answer-commentary-sv     "Jag visste inte att ett intyg behövs :("
                                                   :statement-fi             "Tämän kerran annetaan anteeksi, kun hän ei tiennyt."
                                                   :statement-sv             "Han vet inte. Vi förlotar."
                                                   :osapuoli-specific-data   [{:osapuoli-id        osapuoli-id
                                                                               :hallinto-oikeus-id 1
                                                                               :document           true}
                                                                              {:osapuoli-id        osapuoli-id-2
                                                                               :hallinto-oikeus-id nil
                                                                               :document           false}]
                                                   :department-head-title-fi "Apulaisjohtaja"
                                                   :department-head-title-sv "Apulaisjohtaja på svenska"
                                                   :department-head-name     "Yli Päällikkö"}}
              response (ts/handler (-> (mock/request :post (format "/api/private/valvonta/kaytto/%s/toimenpiteet" valvonta-id))
                                       (mock/json-body new-toimenpide)
                                       (test-kayttajat/with-virtu-user)
                                       (mock/header "Accept" "application/json")))]
          (t/is (true? @html->pdf-called?))
          (t/is (= (:status response) 201))))))


  (t/testing "Preview api call for käskypäätös / varsinainen päätös toimenpide succeeds"
    (t/testing "for yksityishenkilö"
      (let [valvonta-id (valvonta-service/add-valvonta! ts/*db*
                                                        {:katuosoite        "Testitie 5"
                                                         :postinumero       "90100"
                                                         :ilmoituspaikka-id 0})
            osapuoli-id (valvonta-service/add-henkilo! ts/*db*
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
            new-toimenpide {:type-id            8
                            :deadline-date      (str (LocalDate/of 2023 10 4))
                            :template-id        6
                            :description        "Tehdään varsinainen päätös, omistaja vastasi kuulemiskirjeeseen"
                            :type-specific-data {:fine                     857
                                                 :recipient-answered       false
                                                 :answer-commentary-fi     "Hän ei vastannut ollenkaan"
                                                 :answer-commentary-sv     "Han svarade inte alls"
                                                 :statement-fi             "Koska hän ei vastannut ollenkaan hän joutuu maksamaan paljon sakkoja."
                                                 :statement-sv             "Eftersom han inte svarade alls måste han betala mycket böter."
                                                 :osapuoli-specific-data   [{:osapuoli-id        osapuoli-id
                                                                             :hallinto-oikeus-id 3
                                                                             :document           true}]
                                                 :department-head-title-fi "Johtaja"
                                                 :department-head-title-sv "Ledar"
                                                 :department-head-name     "Nimi Muutettu"}}
            response (ts/handler (-> (mock/request :post (format "/api/private/valvonta/kaytto/%s/toimenpiteet/henkilot/%s/preview" valvonta-id osapuoli-id))
                                     (mock/json-body new-toimenpide)
                                     (test-kayttajat/with-virtu-user)
                                     (mock/header "Accept" "application/json")))]
        (t/is (= (:status response) 200))))

    (t/testing "for yritysomistaja"
      (let [valvonta-id (valvonta-service/add-valvonta! ts/*db*
                                                        {:katuosoite        "Testitie 5"
                                                         :postinumero       "90100"
                                                         :ilmoituspaikka-id 0})
            osapuoli-id (valvonta-service/add-yritys! ts/*db*
                                                      valvonta-id
                                                      {:nimi                     "Yritysomistaja"
                                                       :toimitustapa-description nil
                                                       :toimitustapa-id          0
                                                       :email                    nil
                                                       :rooli-id                 0
                                                       :jakeluosoite             "Testikatu 12"
                                                       :vastaanottajan-tarkenne  "Lisäselite C/O"
                                                       :postitoimipaikka         "Helsinki"
                                                       :puhelin                  nil
                                                       :postinumero              "00100"
                                                       :rooli-description        ""
                                                       :maa                      "FI"})
            new-toimenpide {:type-id            8
                            :deadline-date      (str (LocalDate/of 2023 10 4))
                            :template-id        6
                            :description        "Tehdään varsinainen päätös, omistaja vastasi kuulemiskirjeeseen"
                            :type-specific-data {:fine                     857
                                                 :recipient-answered       true
                                                 :answer-commentary-fi     "Yritykseni on niin iso, ettei minun tarvitse välittää tällaisista asioista"
                                                 :answer-commentary-sv     "Mitt företag är så stort att jag inte behöver bry mig om sådana saker"
                                                 :statement-fi             "Vastaus oli väärä, joten saat isot sakot."
                                                 :statement-sv             "Svaret var fel, så du får stora böter."
                                                 :osapuoli-specific-data   [{:osapuoli-id        osapuoli-id
                                                                             :hallinto-oikeus-id 5
                                                                             :document           true}]
                                                 :department-head-title-fi "Titteli"
                                                 :department-head-title-sv "Tittel"
                                                 :department-head-name     "Nimi"}}
            response (ts/handler (-> (mock/request :post (format "/api/private/valvonta/kaytto/%s/toimenpiteet/yritykset/%s/preview" valvonta-id osapuoli-id))
                                     (mock/json-body new-toimenpide)
                                     (test-kayttajat/with-virtu-user)
                                     (mock/header "Accept" "application/json")))]
        (t/is (= (:status response) 200))))))

(t/deftest sakkopaatos-kuulemiskirje-test
  (test-kayttajat/insert-virtu-paakayttaja!
    {:etunimi  "Asian"
     :sukunimi "Tuntija"
     :email    "testi@ara.fi"
     :puhelin  "0504363675457"})
  (t/testing "Preview api call for sakkopäätös / kuulemiskirje toimenpide succeeds"
    (let [valvonta-id (valvonta-service/add-valvonta! ts/*db*
                                                      {:katuosoite        "Testitie 5"
                                                       :postinumero       "90100"
                                                       :ilmoituspaikka-id 0})
          osapuoli-id (valvonta-service/add-henkilo! ts/*db*
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
          new-toimenpide {:type-id            14
                          :deadline-date      (str (LocalDate/of 2023 11 4))
                          :template-id        7
                          :description        "Tehdään sakkopäätöksen kuulemiskirje"
                          :type-specific-data {:fine 9000}}
          response (ts/handler (-> (mock/request :post (format "/api/private/valvonta/kaytto/%s/toimenpiteet/henkilot/%s/preview" valvonta-id osapuoli-id))
                                   (mock/json-body new-toimenpide)
                                   (test-kayttajat/with-virtu-user)
                                   (mock/header "Accept" "application/json")))]
      (t/is (= (:status response) 200))))

  (t/testing "Sakkopäätös / kuulemiskirje toimenpide is created successfully for yksityishenkilö and document is generated with correct information"
    ;; Add the valvonta and previous toimenpides
    (let [valvonta-id (valvonta-service/add-valvonta! ts/*db* {:katuosoite        "Testitie 5"
                                                               :postinumero       "90100"
                                                               :ilmoituspaikka-id 0})
          kehotus-timestamp (-> (LocalDate/of 2023 6 12)
                                (.atStartOfDay (ZoneId/systemDefault))
                                .toInstant)
          varoitus-timestamp (-> (LocalDate/of 2023 7 13)
                                 (.atStartOfDay (ZoneId/systemDefault))
                                 .toInstant)
          kuulemiskirje-timestamp (-> (LocalDate/of 2023 8 13)
                                      (.atStartOfDay (ZoneId/systemDefault))
                                      .toInstant)
          varsinainen-paatos-timestamp (-> (LocalDate/of 2023 9 13)
                                           (.atStartOfDay (ZoneId/systemDefault))
                                           .toInstant)
          html->pdf-called? (atom false)
          ;; Add osapuoli to the valvonta
          osapuoli-id (valvonta-service/add-henkilo!
                        ts/*db*
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
                         :maa                      "FI"})]
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

      ;; Add käskypäätös / kuulemiskirje toimenpide to the valvonta
      (jdbc/insert! ts/*db* :vk_toimenpide {:valvonta_id        valvonta-id
                                            :type_id            7
                                            :create_time        kuulemiskirje-timestamp
                                            :publish_time       kuulemiskirje-timestamp
                                            :deadline_date      (LocalDate/of 2023 8 27)
                                            :type_specific_data {:fine 9000}
                                            :diaarinumero       "ARA-05.03.01-2023-159"})
      ;; Add käskypäätös / varsinainen päätös toimenpide to the valvonta
      (jdbc/insert! ts/*db* :vk_toimenpide {:valvonta_id        valvonta-id
                                            :type_id            8
                                            :create_time        varsinainen-paatos-timestamp
                                            :publish_time       varsinainen-paatos-timestamp
                                            :deadline_date      (LocalDate/of 2023 10 4)
                                            :template_id        6
                                            :description        "Tehdään varsinainen päätös, omistaja vastasi kuulemiskirjeeseen"
                                            :type_specific_data {:fine                     857
                                                                 :recipient-answered       true
                                                                 :answer-commentary-fi     "En tiennyt, että todistus tarvitaan :("
                                                                 :answer-commentary-sv     "Jag visste inte att ett intyg behövs :("
                                                                 :statement-fi             "Tämän kerran annetaan anteeksi, kun hän ei tiennyt."
                                                                 :statement-sv             "Han vet inte. Vi förlotar."
                                                                 :osapuoli-specific-data   [{:osapuoli-id        osapuoli-id
                                                                                             :hallinto-oikeus-id 1
                                                                                             :document           true}]
                                                                 :department-head-title-fi "Apulaisjohtaja"
                                                                 :department-head-title-sv "Apulaisjohtaja på svenska"
                                                                 :department-head-name     "Yli Päällikkö"}})

      ;; Mock the current time to ensure that the document has a fixed date
      (with-bindings {#'time/clock    (Clock/fixed (-> (LocalDate/of 2023 10 5)
                                                       (.atStartOfDay time/timezone)
                                                       .toInstant)
                                                   time/timezone)
                      #'pdf/html->pdf (partial html->pdf-with-assertion
                                               "documents/sakkopaatos-kuulemiskirje-yksityishenkilo.html"
                                               html->pdf-called?)}
        (let [new-toimenpide {:type-id            14
                              :deadline-date      (str (LocalDate/of 2023 11 4))
                              :template-id        7
                              :description        "Tehdään sakkopäätöksen kuulemiskirje"
                              :type-specific-data {:fine 9000}}
              response (ts/handler (-> (mock/request :post (format "/api/private/valvonta/kaytto/%s/toimenpiteet" valvonta-id))
                                       (mock/json-body new-toimenpide)
                                       (test-kayttajat/with-virtu-user)
                                       (mock/header "Accept" "application/json")))]
          (t/is (true? @html->pdf-called?))
          (t/is (= (:status response) 201)))))))

(t/deftest kaskypaatos-odotetaan-valitusajan-umpeutumista-test
  (test-kayttajat/insert-virtu-paakayttaja!
    {:etunimi  "Asian"
     :sukunimi "Tuntija"
     :email    "testi@ara.fi"
     :puhelin  "0504363675457"})
  (t/testing "Käskypäätös / odotetaan valitusajan umpeutuminen toimenpide is created successfully"
    (let [valvonta-id (valvonta-service/add-valvonta! ts/*db*
                                                      {:katuosoite        "Testitie 5"
                                                       :postinumero       "90100"
                                                       :ilmoituspaikka-id 0})
          new-toimenpide {:type-id       12
                          :deadline-date (str (LocalDate/of 2023 11 4))
                          :template-id   nil
                          :description   "Odotetaan valitusajan päättymistä"}
          response (ts/handler (-> (mock/request :post (format "/api/private/valvonta/kaytto/%s/toimenpiteet" valvonta-id))
                                   (mock/json-body new-toimenpide)
                                   (test-kayttajat/with-virtu-user)
                                   (mock/header "Accept" "application/json")))]
      (t/is (= (:status response) 201))

      (t/testing "Toimenpide is returned through the api"
        (let [response (ts/handler (-> (mock/request :get (format "/api/private/valvonta/kaytto/%s/toimenpiteet" valvonta-id))
                                       (test-kayttajat/with-virtu-user)
                                       (mock/header "Accept" "application/json")))
              response-body (j/read-value (:body response) j/keyword-keys-object-mapper)]
          (t/is (= (:status response) 200))
          (t/is (= (count response-body) 1))
          (t/is (= (-> response-body
                       first
                       (dissoc :publish-time :create-time))
                   {:author             {:etunimi  "Asian"
                                         :id       1
                                         :rooli-id 2
                                         :sukunimi "Tuntija"}
                    :deadline-date      "2023-11-04"
                    :description        "Odotetaan valitusajan päättymistä"
                    :diaarinumero       nil
                    :filename           nil
                    :henkilot           []
                    :id                 1
                    :template-id        nil
                    :type-id            12
                    :type-specific-data nil
                    :valvonta-id        1
                    :yritykset          []})))))))

(t/deftest kaskypaatos-valitusaika-umpeutunut-test
  (test-kayttajat/insert-virtu-paakayttaja!
    {:etunimi  "Asian"
     :sukunimi "Tuntija"
     :email    "testi@ara.fi"
     :puhelin  "0504363675457"})
  (t/testing "Käskypäätös / valitusaika umpeutunut toimenpide is created successfully"
    (let [valvonta-id (valvonta-service/add-valvonta! ts/*db*
                                                      {:katuosoite        "Testitie 5"
                                                       :postinumero       "90100"
                                                       :ilmoituspaikka-id 0})
          new-toimenpide {:type-id       13
                          :deadline-date nil
                          :template-id   nil
                          :description   "Valitusaika umpeutui"}
          response (ts/handler (-> (mock/request :post (format "/api/private/valvonta/kaytto/%s/toimenpiteet" valvonta-id))
                                   (mock/json-body new-toimenpide)
                                   (test-kayttajat/with-virtu-user)
                                   (mock/header "Accept" "application/json")))]
      (t/is (= (:status response) 201))

      (t/testing "Toimenpide is returned through the api"
        (let [response (ts/handler (-> (mock/request :get (format "/api/private/valvonta/kaytto/%s/toimenpiteet" valvonta-id))
                                       (test-kayttajat/with-virtu-user)
                                       (mock/header "Accept" "application/json")))
              response-body (j/read-value (:body response) j/keyword-keys-object-mapper)]
          (t/is (= (:status response) 200))
          (t/is (= (count response-body) 1))
          (t/is (= (-> response-body
                       first
                       (dissoc :publish-time :create-time))
                   {:author             {:etunimi  "Asian"
                                         :id       1
                                         :rooli-id 2
                                         :sukunimi "Tuntija"}
                    :deadline-date      nil
                    :description        "Valitusaika umpeutui"
                    :diaarinumero       nil
                    :filename           nil
                    :henkilot           []
                    :id                 1
                    :template-id        nil
                    :type-id            13
                    :type-specific-data nil
                    :valvonta-id        1
                    :yritykset          []})))))))

(t/deftest adding-and-fetching-valvonta
  (let [kayttaja-id (test-kayttajat/insert-virtu-paakayttaja!)
        valvonta (-> {}
                     (generators/complete valvonta-schema/ValvontaSave)
                     (non-nil-key->string :havaintopaiva)   ; Jackson has problems encoding local date
                     (assoc :ilmoituspaikka-id 1)
                     (assoc :valvoja-id kayttaja-id))]
    (t/testing "Uuden valvonnan luominen"
      (let [response (ts/handler (-> (mock/request :post "/api/private/valvonta/kaytto")
                                     (mock/json-body valvonta)
                                     (test-kayttajat/with-virtu-user)
                                     (mock/header "Accept" "application/json")))
            response-body (j/read-value (:body response) j/keyword-keys-object-mapper)]
        (t/is (= (:status response) 201))
        (t/is (= response-body {:id 1}))))
    (t/testing "Luotu valvonta on tallennettu ja voidaan hakea"
      (let [fetch-response (ts/handler (-> (mock/request :get (format "/api/private/valvonta/kaytto/1"))
                                           (test-kayttajat/with-virtu-user)
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
        toimenpiteet (->> (repeatedly 12 (fn [] (generators/complete {:type-id 1} valvonta-schema/ToimenpideAdd)))
                          (map vector (flatten (repeatedly (constantly [1 2]))))
                          (map (fn [[template-id toimenpide]] (assoc toimenpide :template-id template-id)))
                          (map vector (flatten (repeat (map :id valvonnat)))))]
    (doseq [[valvonta-id toimenpide] toimenpiteet]
      (add-toimenpide-and-map-id! valvonta-id toimenpide))
    (t/testing "Valvonnalle palautetaan 6 toimenpidettä"
      (let [response (ts/handler (-> (mock/request :get (format "/api/private/valvonta/kaytto/%s/toimenpiteet" (-> valvonnat first :id)))
                                     (test-kayttajat/with-virtu-user)
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
        toimenpiteet (->> (repeatedly 2 (fn [] (generators/complete {:type-id 1} valvonta-schema/ToimenpideAdd)))
                          (map vector (flatten (repeatedly (constantly [1 2])))) ; Use two different templates for every other toimenpide
                          (map (fn [[template-id toimenpide]] (assoc toimenpide :template-id template-id)))
                          (map vector (flatten (repeat (map :id valvonnat))))
                          (mapv #(apply add-toimenpide-and-map-id! %)))]
    (t/testing "Hae valvontojen määrä joissa on käytetty asiakirjapohjaa 1"
      (let [response (ts/handler (-> (mock/request :get "/api/private/valvonta/kaytto/count")
                                     (mock/query-string {:asiakirjapohja-id 1})
                                     (test-kayttajat/with-virtu-user)
                                     (mock/header "Accept" "application/json")))
            response-body (j/read-value (:body response) j/keyword-keys-object-mapper)]
        (t/is (= (:status response) 200))
        (t/is (= response-body {:count 1}))))
    (t/testing "Hae valvonnat joissa on käytetty asiakirjapohjaa 1"
      (let [response (ts/handler (-> (mock/request :get "/api/private/valvonta/kaytto")
                                     (mock/query-string {:asiakirjapohja-id 1})
                                     (test-kayttajat/with-virtu-user)
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
