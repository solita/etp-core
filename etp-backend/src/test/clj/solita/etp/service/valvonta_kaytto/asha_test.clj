(ns solita.etp.service.valvonta-kaytto.asha-test
  (:require [clojure.test :as t]
            [solita.etp.service.valvonta-kaytto.asha :as asha]
            [solita.etp.test-system :as ts]))

(t/use-fixtures :each ts/fixture)

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
