(ns solita.etp.valvonta-kaytto.sakkopaatos-kuulemiskirje-test
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.test :as t]
    [ring.mock.request :as mock]
    [solita.common.time :as time]
    [solita.etp.document-assertion :refer [html->pdf-with-assertion]]
    [solita.etp.service.pdf :as pdf]
    [solita.etp.service.valvonta-kaytto :as valvonta-service]
    [solita.etp.test-data.kayttaja :as test-kayttajat]
    [solita.etp.test-system :as ts])
  (:import (java.time Clock LocalDate ZoneId)))

(t/use-fixtures :each ts/fixture)

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
                                                                                             :osapuoli-type      "henkilo"
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
