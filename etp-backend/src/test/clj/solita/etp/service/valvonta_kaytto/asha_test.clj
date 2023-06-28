(ns solita.etp.service.valvonta-kaytto.asha-test
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.test :as t]
            [solita.etp.service.valvonta-kaytto :as valvonta-service]
            [solita.etp.service.valvonta-kaytto.asha :as asha]
            [solita.etp.test-system :as ts])
  (:import (java.time LocalDate ZoneId)))

(t/use-fixtures :each ts/fixture)

(t/deftest past-dates-for-kaskypaatos-kuulemiskirje-test
  (t/testing "Correct dates for the valvonta are found from database and they are formatted in Finnish date format"
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
      ;; and deadline are found and formatted correctly
      (t/is (= (asha/past-dates-for-kaskypaatos-kuulemiskirje ts/*db* valvonta-id)
               {:kehotus-pvm         "12.06.2023"
                :kehotus-maarapaiva  "12.07.2023"
                :varoitus-pvm        "13.07.2023"
                :varoitus-maarapaiva "13.08.2023"}))))

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
      (t/is (= (asha/past-dates-for-kaskypaatos-kuulemiskirje ts/*db* valvonta-id)
               {:kehotus-pvm         "12.06.2023"
                :kehotus-maarapaiva  "12.07.2023"
                :varoitus-pvm        "13.07.2023"
                :varoitus-maarapaiva "13.08.2023"})))))
