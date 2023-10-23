(ns solita.etp.service.valvonta-kaytto.asha-test
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.test :as t]
            [solita.etp.service.luokittelu :as luokittelu]
            [solita.etp.service.valvonta-kaytto :as valvonta-service]
            [solita.etp.service.valvonta-kaytto.asha :as asha]
            [solita.etp.test-system :as ts])
  (:import (java.time LocalDate ZoneId)))

(t/use-fixtures :each ts/fixture)

(t/deftest past-dates-for-kaskypaatos-kuulemiskirje-test
  (t/testing "Correct dates for the kehotus and valvonta are found from database and they are formatted in Finnish date format"
    (let [valvonta-id (valvonta-service/add-valvonta! ts/*db* {:katuosoite "Testitie 5"})
          kehotus-timestamp (-> (LocalDate/of 2023 6 12)
                                (.atStartOfDay (ZoneId/systemDefault))
                                .toInstant)
          varoitus-timestamp (-> (LocalDate/of 2023 7 13)
                                 (.atStartOfDay (ZoneId/systemDefault))
                                 .toInstant)]

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

      ;; Kehotus creation date and deadline and varoitus creation date
      ;; and deadline are found and formatted correctly.
      ;; There is no kuulemiskirje or varsinainen päätös toimenpide yet
      (t/is (= (asha/past-dates-for-kaskypaatos-toimenpiteet ts/*db* valvonta-id)
               {:kehotus-pvm                   "12.06.2023"
                :kehotus-maarapaiva            "12.07.2023"
                :varoitus-pvm                  "13.07.2023"
                :varoitus-maarapaiva           "13.08.2023"
                :kuulemiskirje-pvm             nil
                :kuulemiskirje-maarapaiva      nil
                :varsinainen-paatos-pvm        nil
                :varsinainen-paatos-maarapaiva nil}))))

  (t/testing "When there are multiple kehotus and varoitus toimenpide, the newest dates are found"
    (let [valvonta-id (valvonta-service/add-valvonta! ts/*db* {:katuosoite "Testitie 5"})
          kehotus-timestamp (-> (LocalDate/of 2023 6 12)
                                (.atStartOfDay (ZoneId/systemDefault))
                                .toInstant)
          varoitus-timestamp (-> (LocalDate/of 2023 7 13)
                                 (.atStartOfDay (ZoneId/systemDefault))
                                 .toInstant)]

      ;; Add old kehotus-toimenpide
      (jdbc/insert! ts/*db* :vk_toimenpide {:valvonta_id   valvonta-id
                                            :type_id       2
                                            :create_time   (-> (LocalDate/of 2022 6 11)
                                                               (.atStartOfDay (ZoneId/systemDefault))
                                                               .toInstant)
                                            :publish_time  (-> (LocalDate/of 2022 6 11)
                                                               (.atStartOfDay (ZoneId/systemDefault))
                                                               .toInstant)
                                            :deadline_date (LocalDate/of 2022 7 11)})

      ;; Add old varoitus-toimenpide
      (jdbc/insert! ts/*db* :vk_toimenpide {:valvonta_id   valvonta-id
                                            :type_id       3
                                            :create_time   (-> (LocalDate/of 2022 7 12)
                                                               (.atStartOfDay (ZoneId/systemDefault))
                                                               .toInstant)
                                            :publish_time  (-> (LocalDate/of 2022 7 12)
                                                               (.atStartOfDay (ZoneId/systemDefault))
                                                               .toInstant)
                                            :deadline_date (LocalDate/of 2022 8 12)})

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

      ;; The newest kehotus creation date and deadline and varoitus creation date
      ;; and deadline are found and formatted correctly
      ;; There is no kuulemiskirje toimenpide yet
      (t/is (= (asha/past-dates-for-kaskypaatos-toimenpiteet ts/*db* valvonta-id)
               {:kehotus-pvm                   "12.06.2023"
                :kehotus-maarapaiva            "12.07.2023"
                :varoitus-pvm                  "13.07.2023"
                :varoitus-maarapaiva           "13.08.2023"
                :kuulemiskirje-pvm             nil
                :kuulemiskirje-maarapaiva      nil
                :varsinainen-paatos-pvm        nil
                :varsinainen-paatos-maarapaiva nil}))))

  (t/testing "Correct dates for all käskypäätös toimeenpiteet are found from database and they are formatted in Finnish date format"
    (let [valvonta-id (valvonta-service/add-valvonta! ts/*db* {:katuosoite "Testitie 5"})
          kehotus-timestamp (-> (LocalDate/of 2023 6 12)
                                (.atStartOfDay (ZoneId/systemDefault))
                                .toInstant)
          varoitus-timestamp (-> (LocalDate/of 2023 7 13)
                                 (.atStartOfDay (ZoneId/systemDefault))
                                 .toInstant)
          kuulemiskirje-timestamp (-> (LocalDate/of 2023 8 14)
                                      (.atStartOfDay (ZoneId/systemDefault))
                                      .toInstant)
          varsinainen-paatos-timestamp (-> (LocalDate/of 2023 9 14)
                                      (.atStartOfDay (ZoneId/systemDefault))
                                      .toInstant)]

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

      ;; Add kuulemiskirje-toimenpide to the valvonta
      (jdbc/insert! ts/*db* :vk_toimenpide {:valvonta_id   valvonta-id
                                            :type_id       7
                                            :create_time   kuulemiskirje-timestamp
                                            :publish_time  kuulemiskirje-timestamp
                                            :deadline_date (LocalDate/of 2023 8 28)})

      ;; Add varsinainen päätös -toimenpide to the valvonta
      (jdbc/insert! ts/*db* :vk_toimenpide {:valvonta_id   valvonta-id
                                            :type_id       8
                                            :create_time   varsinainen-paatos-timestamp
                                            :publish_time  varsinainen-paatos-timestamp
                                            :deadline_date (LocalDate/of 2023 10 28)})
      ;; Kehotus creation date and deadline and varoitus creation date
      ;; and deadline are found and formatted correctly
      (t/is (= (asha/past-dates-for-kaskypaatos-toimenpiteet ts/*db* valvonta-id)
               {:kehotus-pvm                   "12.06.2023"
                :kehotus-maarapaiva            "12.07.2023"
                :varoitus-pvm                  "13.07.2023"
                :varoitus-maarapaiva           "13.08.2023"
                :kuulemiskirje-pvm             "14.08.2023"
                :kuulemiskirje-maarapaiva      "28.08.2023"
                :varsinainen-paatos-pvm        "14.09.2023"
                :varsinainen-paatos-maarapaiva "28.10.2023"})))))

(t/deftest kuulemiskirje-data-test
  (let [valvonta-id (valvonta-service/add-valvonta! ts/*db* {:katuosoite "Testitie 5"})
        kuulemiskirje-timestamp (-> (LocalDate/of 2023 8 14)
                                    (.atStartOfDay (ZoneId/systemDefault))
                                    .toInstant)]
    (jdbc/insert! ts/*db*
                  :vk_toimenpide
                  {:valvonta_id        valvonta-id
                   :type_id            7
                   :create_time        kuulemiskirje-timestamp
                   :publish_time       kuulemiskirje-timestamp
                   :deadline_date      (LocalDate/of 2023 8 28)
                   :diaarinumero       "ARA-05.03.01-2023-238"
                   :type_specific_data {:fine 6100}})

    (t/testing "When kuulemiskirje exists for the given valvonta, correct diaarinumero and fine are returned"
      (t/is (= (asha/kuulemiskirje-data ts/*db* valvonta-id)
               {:kuulemiskirje-diaarinumero "ARA-05.03.01-2023-238"
                :kuulemiskirje-fine         6100})))

    (t/testing "When multiple kuulemiskirje exists for the given valvonta, diaari of the newest is returned"
      (jdbc/insert! ts/*db*
                    :vk_toimenpide
                    {:valvonta_id        valvonta-id
                     :type_id            7
                     :create_time        (-> (LocalDate/of 2023 8 10)
                                             (.atStartOfDay (ZoneId/systemDefault))
                                             .toInstant)
                     :publish_time       (-> (LocalDate/of 2023 8 10)
                                             (.atStartOfDay (ZoneId/systemDefault))
                                             .toInstant)
                     :deadline_date      (LocalDate/of 2023 8 28)
                     :diaarinumero       "ARA-05.03.01-2023-235"
                     :type_specific_data {:fine 6100}})

      (jdbc/insert! ts/*db*
                    :vk_toimenpide
                    {:valvonta_id        valvonta-id
                     :type_id            7
                     :create_time        (-> (LocalDate/of 2023 8 19)
                                             (.atStartOfDay (ZoneId/systemDefault))
                                             .toInstant)
                     :publish_time       (-> (LocalDate/of 2023 8 19)
                                             (.atStartOfDay (ZoneId/systemDefault))
                                             .toInstant)
                     :deadline_date      (LocalDate/of 2023 8 28)
                     :diaarinumero       "ARA-05.03.01-2023-245"
                     :type_specific_data {:fine 3200}})
      (t/is (= (asha/kuulemiskirje-data ts/*db* valvonta-id)
               {:kuulemiskirje-diaarinumero "ARA-05.03.01-2023-245"
                :kuulemiskirje-fine         3200})))))

(t/deftest format-type-specific-data-test
  (t/testing "For käskypäätös / varsinainen päätös toimenpide a new key vastaus is added and its value is based on values of :recipient-answered and :answer-commentary"
    (t/is (= (asha/format-type-specific-data
               ts/*db*
               {:type-id            8
                :type-specific-data {:fine                     129
                                     :osapuoli-specific-data   [{:osapuoli-id        1
                                                                 :hallinto-oikeus-id 0
                                                                 :document           true
                                                                 :recipient-answered       true
                                                                 :answer-commentary-fi     "Voi anteeksi, en tiennyt."
                                                                 :answer-commentary-sv     "Jag vet inte, förlåt."
                                                                 :statement-fi             "Olisi pitänyt tietää."
                                                                 :statement-sv             "Du måste ha visst."}]
                                     :department-head-name     "Jorma Jormanen"
                                     :department-head-title-fi "Hallinto-oikeuden presidentti"
                                     :department-head-title-sv "Hallinto-oikeuden kuningas"}}
               1)
             {:fine                     129
              :vastaus-fi               "Asianosainen antoi vastineen kuulemiskirjeeseen. Voi anteeksi, en tiennyt."
              :oikeus-fi                "Helsingin hallinto-oikeudelta"
              :vastaus-sv               "gav ett bemötande till brevet om hörande. Jag vet inte, förlåt."
              :statement-fi             "Olisi pitänyt tietää."
              :statement-sv             "Du måste ha visst."
              :oikeus-sv                "Helsingfors"
              :department-head-name     "Jorma Jormanen"
              :department-head-title-fi "Hallinto-oikeuden presidentti"
              :department-head-title-sv "Hallinto-oikeuden kuningas"
              :recipient-answered true}))

    (t/testing "For käskypäätös / kuulemiskirje toimenpide :type-spefic-data map is returned as is, as no special formatting is needed"
      (t/is (= (asha/format-type-specific-data
                 ts/*db*
                 {:type-id            7
                  :type-specific-data {:fine 800}}
                 nil)
               {:fine 800})))))

(t/deftest hallinto-oikeus-id->formatted-string-test
  (t/testing "id 0 results in Helsingin hallinto-oikeudelta"
    (t/is (= (asha/hallinto-oikeus-id->formatted-strings ts/*db* 0)
             {:fi "Helsingin hallinto-oikeudelta"
              :sv "Helsingfors"})))

  (t/testing "id 1 results in Hämeenlinnan hallinto-oikeudelta"
    (t/is (= (asha/hallinto-oikeus-id->formatted-strings ts/*db* 1)
             {:fi "Hämeenlinnan hallinto-oikeudelta"
              :sv "Tavastehus"})))

  (t/testing "id 2 results in Itä-Suomen hallinto-oikeudelta"
    (t/is (= (asha/hallinto-oikeus-id->formatted-strings ts/*db* 2)
             {:fi "Itä-Suomen hallinto-oikeudelta"
              :sv "Östra Finland"})))

  (t/testing "id 3 results in Pohjois-Suomen hallinto-oikeudelta"
    (t/is (= (asha/hallinto-oikeus-id->formatted-strings ts/*db* 3)
             {:fi "Pohjois-Suomen hallinto-oikeudelta"
              :sv "Norra Finland"})))

  (t/testing "id 4 results in Turun hallinto-oikeudelta"
    (t/is (= (asha/hallinto-oikeus-id->formatted-strings ts/*db* 4)
             {:fi "Turun hallinto-oikeudelta"
              :sv "Åbo"})))

  (t/testing "id 5 results in Vaasan hallinto-oikeudelta"
    (t/is (= (asha/hallinto-oikeus-id->formatted-strings ts/*db* 5)
             {:fi "Vaasan hallinto-oikeudelta"
              :sv "Vasa"})))

  (t/testing "Unknown id results in exception"
    (t/is (thrown-with-msg?
            Exception
            #"Unknown hallinto-oikeus-id: 6" (asha/hallinto-oikeus-id->formatted-strings ts/*db* 6))))

  (t/testing "All hallinto-oikeudet in database have a formatted string"
    (let [hallinto-oikeudet (luokittelu/find-hallinto-oikeudet ts/*db*)]
      (t/is (= (count hallinto-oikeudet)
               6))

      (doseq [hallinto-oikeus hallinto-oikeudet]
        (t/is (not (nil? (:fi (asha/hallinto-oikeus-id->formatted-strings ts/*db* (:id hallinto-oikeus))))))
        (t/is (not (nil? (:sv (asha/hallinto-oikeus-id->formatted-strings ts/*db* (:id hallinto-oikeus))))))))))

(t/deftest find-court-id-from-osapuoli-specific-data-test
  (t/testing "Correct court id is found for the osapuoli"
    (t/is (= (asha/find-administrative-court-id-from-osapuoli-specific-data
               [{:osapuoli-id        1
                 :hallinto-oikeus-id 0}
                {:osapuoli-id        3
                 :hallinto-oikeus-id 5}
                {:osapuoli-id        643
                 :hallinto-oikeus-id 2}]
               3)
             5))))

(t/deftest remove-osapuolet-with-no-document-if-varsinainen-paatos-test
  (t/testing "Two osapuolis, one hallinto-oikeus, only the osapuoli with the hallinto-oikeus should be returned"
    (let [osapuolet [{:toimitustapa-description nil
                      :toimitustapa-id          0
                      :email                    nil
                      :rooli-id                 0
                      :jakeluosoite             "Testikatu 12"
                      :valvonta-id              3
                      :postitoimipaikka         "Helsinki"
                      :puhelin                  nil
                      :sukunimi                 "Talonomistaja"
                      :postinumero              "00100"
                      :id                       2
                      :henkilotunnus            "000000-0000"
                      :rooli-description        ""
                      :etunimi                  "Testi"
                      :vastaanottajan-tarkenne  nil
                      :maa                      "FI"}
                     {:toimitustapa-description nil
                      :toimitustapa-id          0
                      :email                    nil
                      :rooli-id                 0
                      :jakeluosoite             "Testikatu 13"
                      :valvonta-id              3
                      :postitoimipaikka         "Stockholm"
                      :puhelin                  nil
                      :sukunimi                 "Omistaja"
                      :postinumero              "00000"
                      :id                       3
                      :henkilotunnus            "000000-0001"
                      :rooli-description        ""
                      :etunimi                  "Toinen"
                      :vastaanottajan-tarkenne  nil
                      :maa                      "SV"}]
          toimenpide {:description
                      "Tehdään varsinainen päätös, omistaja vastasi kuulemiskirjeeseen"
                      :type-id      8
                      :valvonta-id  3
                      :filename     nil
                      :diaarinumero "ARA-05.03.01-2023-159"
                      :id           12
                      :author-id    1
                      :type-specific-data
                      {:department-head-title-fi "Apulaisjohtaja"
                       :department-head-name     "Yli Päällikkö"
                       :osapuoli-specific-data   [{:hallinto-oikeus-id 1
                                                   :osapuoli-id        2
                                                   :document           true}
                                                  {:hallinto-oikeus-id nil
                                                   :osapuoli-id        3
                                                   :document           false}]
                       :recipient-answered       true
                       :statement-sv             "Han vet inte. Vi förlotar."
                       :statement-fi             "Tämän kerran annetaan anteeksi kun hän ei tiennyt."
                       :department-head-title-sv "Apulaisjohtaja på svenska"
                       :fine                     857
                       :answer-commentary-sv     "Jag visste inte att ett intyg behövs :("
                       :answer-commentary-fi     "En tiennyt, että todistus tarvitaan :("}
                      :template-id  6}]
      (t/is (= (asha/remove-osapuolet-with-no-document toimenpide osapuolet)
               [{:toimitustapa-description nil
                 :toimitustapa-id          0
                 :email                    nil
                 :rooli-id                 0
                 :jakeluosoite             "Testikatu 12"
                 :valvonta-id              3
                 :postitoimipaikka         "Helsinki"
                 :puhelin                  nil
                 :sukunimi                 "Talonomistaja"
                 :postinumero              "00100"
                 :id                       2
                 :henkilotunnus            "000000-0000"
                 :rooli-description        ""
                 :etunimi                  "Testi"
                 :vastaanottajan-tarkenne  nil
                 :maa                      "FI"}]))))

  (t/testing "Two osapuolis and two hallinto-oikeus selections, both osapuolet are returned"
    (t/is (= (asha/remove-osapuolet-with-no-document
               {:type-id            8
                :type-specific-data {:osapuoli-specific-data [{:hallinto-oikeus-id 1
                                                               :osapuoli-id        2
                                                               :document           true}
                                                              {:hallinto-oikeus-id 3
                                                               :osapuoli-id        3
                                                               :document           true}]}}
               [{:id 2}
                {:id 3}])
             [{:id 2}
              {:id 3}])))

  (t/testing "Two osapuolis and toimenpide is not varsinainen päätös, so both should be returned"
    (t/is (= (asha/remove-osapuolet-with-no-document
               {:type-id 7}
               [{:id 2}
                {:id 3}])
             [{:id 2}
              {:id 3}]))))
