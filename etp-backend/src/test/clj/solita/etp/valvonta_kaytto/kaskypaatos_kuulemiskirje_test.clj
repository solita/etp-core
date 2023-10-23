(ns solita.etp.valvonta-kaytto.kaskypaatos-kuulemiskirje-test
  (:require
    [clojure.java.io :as io]
    [clojure.java.jdbc :as jdbc]
    [clojure.test :as t]
    [ring.mock.request :as mock]
    [solita.common.time :as time]
    [solita.etp.service.pdf :as pdf]
    [solita.etp.service.valvonta-kaytto :as valvonta-service]
    [solita.etp.test-data.kayttaja :as test-kayttajat]
    [solita.etp.test-system :as ts])
  (:import (java.time Clock LocalDate ZoneId)))

(t/use-fixtures :each ts/fixture)

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
