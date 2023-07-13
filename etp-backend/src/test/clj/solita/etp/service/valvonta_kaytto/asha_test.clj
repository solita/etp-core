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
      ;; There is no kuulemiskirje toimenpide yet
      (t/is (= (asha/past-dates-for-kaskypaatos-toimenpiteet ts/*db* valvonta-id)
               {:kehotus-pvm              "12.06.2023"
                :kehotus-maarapaiva       "12.07.2023"
                :varoitus-pvm             "13.07.2023"
                :varoitus-maarapaiva      "13.08.2023"
                :kuulemiskirje-pvm        nil
                :kuulemiskirje-maarapaiva nil}))))

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
               {:kehotus-pvm              "12.06.2023"
                :kehotus-maarapaiva       "12.07.2023"
                :varoitus-pvm             "13.07.2023"
                :varoitus-maarapaiva      "13.08.2023"
                :kuulemiskirje-pvm        nil
                :kuulemiskirje-maarapaiva nil}))))

  (t/testing "Correct dates for kehotus, valvonta and kuulemiskirje are found from database and they are formatted in Finnish date format"
    (let [valvonta-id (valvonta-service/add-valvonta! ts/*db* {:katuosoite "Testitie 5"})
          kehotus-timestamp (-> (LocalDate/of 2023 6 12)
                                (.atStartOfDay (ZoneId/systemDefault))
                                .toInstant)
          varoitus-timestamp (-> (LocalDate/of 2023 7 13)
                                 (.atStartOfDay (ZoneId/systemDefault))
                                 .toInstant)
          kuulemiskirje-timestamp (-> (LocalDate/of 2023 8 14)
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
      ;; Kehotus creation date and deadline and varoitus creation date
      ;; and deadline are found and formatted correctly
      (t/is (= (asha/past-dates-for-kaskypaatos-toimenpiteet ts/*db* valvonta-id)
               {:kehotus-pvm              "12.06.2023"
                :kehotus-maarapaiva       "12.07.2023"
                :varoitus-pvm             "13.07.2023"
                :varoitus-maarapaiva      "13.08.2023"
                :kuulemiskirje-pvm        "14.08.2023"
                :kuulemiskirje-maarapaiva "28.08.2023"})))))

(t/deftest format-type-specific-data-test
  (t/testing "For käskypäätös / varsinainen päätös toimenpide a new key vastaus is added and its value is based on values of :recipient-answered and :answer-commentary"
    (t/is (= (asha/format-type-specific-data
               {:type-id            8
                :type-specific-data {:fine               129
                                     :recipient-answered true
                                     :answer-commentary  "Voi anteeksi, en tiennyt."
                                     :court              0}})
             {:fine               129
              :recipient-answered true
              :answer-commentary  "Voi anteeksi, en tiennyt."
              :vastaus            "Asianosainen antoi vastineen kuulemiskirjeeseen. Voi anteeksi, en tiennyt."
              :court              "Helsingin hallinto-oikeudelta"}))

    (t/testing "For käskypäätös / kuulemiskirje toimenpide :type-spefic-data map is returned as is, as no special formatting is needed"
      (t/is (= (asha/format-type-specific-data
                 {:type-id            7
                  :type-specific-data {:fine 800}})
               {:fine 800})))))

(t/deftest hallinto-oikeus-id->formatted-string-test
  (t/testing "id 0 results in Helsingin hallinto-oikeudelta"
    (t/is (= (asha/hallinto-oikeus-id->formatted-string 0)
             "Helsingin hallinto-oikeudelta")))

  (t/testing "id 1 results in Hämeenlinnan hallinto-oikeudelta"
    (t/is (= (asha/hallinto-oikeus-id->formatted-string 1)
             "Hämeenlinnan hallinto-oikeudelta")))

  (t/testing "id 2 results in Itä-Suomen hallinto-oikeudelta"
    (t/is (= (asha/hallinto-oikeus-id->formatted-string 2)
             "Itä-Suomen hallinto-oikeudelta")))

  (t/testing "id 3 results in Pohjois-Suomen hallinto-oikeudelta"
    (t/is (= (asha/hallinto-oikeus-id->formatted-string 3)
             "Pohjois-Suomen hallinto-oikeudelta")))

  (t/testing "id 4 results in Turun hallinto-oikeudelta"
    (t/is (= (asha/hallinto-oikeus-id->formatted-string 4)
             "Turun hallinto-oikeudelta")))
  (t/testing "id 5 results in Vaasan hallinto-oikeudelta"
    (t/is (= (asha/hallinto-oikeus-id->formatted-string 5)
             "Vaasan hallinto-oikeudelta")))
  (t/testing "Unknown id results in exception"
    (t/is (thrown-with-msg?
            Exception
            #"Unknown hallinto-oikeus-id: 6" (asha/hallinto-oikeus-id->formatted-string 6)))))
