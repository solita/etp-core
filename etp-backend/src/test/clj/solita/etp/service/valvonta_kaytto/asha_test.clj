(ns solita.etp.service.valvonta-kaytto.asha-test
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.test :as t]
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
                :varsinainen-paatos-maarapaiva nil
                :sakkopaatos-kuulemiskirje-pvm nil
                :sakkopaatos-kuulemiskirje-maarapaiva nil}))))

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
                :varsinainen-paatos-maarapaiva nil
                :sakkopaatos-kuulemiskirje-pvm nil
                :sakkopaatos-kuulemiskirje-maarapaiva nil}))))

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
                                      .toInstant)
          sakkopaatos-kuulemiskirje-timestamp (-> (LocalDate/of 2023 11 1)
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

      ;; Add sakkopäätös / kuulemiskirje -toimenpide to the valvonta
      (jdbc/insert! ts/*db* :vk_toimenpide {:valvonta_id   valvonta-id
                                            :type_id       14
                                            :create_time   sakkopaatos-kuulemiskirje-timestamp
                                            :publish_time  sakkopaatos-kuulemiskirje-timestamp
                                            :deadline_date (LocalDate/of 2023 11 12)})

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
                :varsinainen-paatos-maarapaiva "28.10.2023"
                :sakkopaatos-kuulemiskirje-pvm "01.11.2023"
                :sakkopaatos-kuulemiskirje-maarapaiva "12.11.2023"})))))

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
