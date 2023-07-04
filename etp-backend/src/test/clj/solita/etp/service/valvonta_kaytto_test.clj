(ns solita.etp.service.valvonta-kaytto-test
  (:require [clojure.test :as t]
            [schema.core :as schema]
            [solita.etp.schema.valvonta-kaytto :as valvonta-kaytto-schema]
            [solita.etp.service.valvonta-kaytto :as valvonta-kaytto]
            [solita.etp.test-system :as ts]))

(t/use-fixtures :each ts/fixture)

(t/deftest find-toimenpidetyypit-test
  (let [toimenpidetyypit (valvonta-kaytto/find-toimenpidetyypit ts/*db*)]
    (t/testing "find-toimenpidetyypit returns correct toimenpidetypes"
      (t/is (= toimenpidetyypit
               [{:id                   0
                 :label-fi             "Valvonnan aloitus"
                 :label-sv             "Valvonnan aloitus (sv)"
                 :valid                true
                 :manually-deliverable false
                 :allow-comments       false}
                {:id                   1
                 :label-fi             "Tietopyyntö 2021"
                 :label-sv             "Begäran om uppgifter 2021"
                 :valid                false
                 :manually-deliverable false
                 :allow-comments       false}
                {:id                   2
                 :label-fi             "Kehotus"
                 :label-sv             " Uppmaning"
                 :valid                true
                 :manually-deliverable false
                 :allow-comments       false}
                {:id                   3
                 :label-fi             "Varoitus"
                 :label-sv             "Varning"
                 :valid                true
                 :manually-deliverable false
                 :allow-comments       false}
                {:id                   4
                 :label-fi             "Käskypäätös"
                 :label-sv             "Käskypäätös (sv)"
                 :valid                true
                 :manually-deliverable false
                 :allow-comments       false}
                {:id                   5
                 :label-fi             "Valvonnan lopetus"
                 :label-sv             "Valvonnan lopetus (sv)"
                 :valid                true
                 :manually-deliverable false
                 :allow-comments       false}
                {:id                   6
                 :label-fi             "HaO-käsittely"
                 :label-sv             "HaO-käsittely (sv)"
                 :valid                true
                 :manually-deliverable false
                 :allow-comments       true}
                {:id                   7
                 :label-fi             "Käskypäätös / kuulemiskirje"
                 :label-sv             "Käskypäätös / kuulemiskirje (sv)"
                 :valid                true
                 :manually-deliverable true
                 :allow-comments       true}
                {:id                   8
                 :label-fi             "Käskypäätös / varsinainen päätös"
                 :label-sv             "Käskypäätös / varsinainen päätös (sv)"
                 :valid                true
                 :manually-deliverable true
                 :allow-comments       true}
                {:id                   9
                 :label-fi             "Käskypäätös / tiedoksianto (ensimmäinen postitus)"
                 :label-sv             "Käskypäätös / tiedoksianto (ensimmäinen postitus) (sv)"
                 :valid                true
                 :manually-deliverable false
                 :allow-comments       false}
                {:id                   10
                 :label-fi             "Käskypäätös / tiedoksianto (toinen postitus)"
                 :label-sv             "Käskypäätös / tiedoksianto (toinen postitus) (sv)"
                 :valid                true
                 :manually-deliverable false
                 :allow-comments       false}
                {:id                   11
                 :label-fi             "Käskypäätös / tiedoksianto (Haastemies)"
                 :label-sv             "Käskypäätös / tiedoksianto (Haastemies) (sv)"
                 :valid                true
                 :manually-deliverable false
                 :allow-comments       false}
                {:id                   12
                 :label-fi             "Käskypäätös / odotetaan valitusajan umpeutumista"
                 :label-sv             "Käskypäätös / odotetaan valitusajan umpeutumista (sv)"
                 :valid                true
                 :manually-deliverable false
                 :allow-comments       false}
                {:id                   13
                 :label-fi             "Käskypäätös / valitusaika umpeutunut"
                 :label-sv             "Käskypäätös / valitusaika umpeutunut (sv)"
                 :valid                true
                 :manually-deliverable false
                 :allow-comments       false}
                {:id                   14
                 :label-fi             "Sakkopäätös / kuulemiskirje"
                 :label-sv             "Sakkopäätös / kuulemiskirje (sv)"
                 :valid                true
                 :manually-deliverable false
                 :allow-comments       false}
                {:id                   15
                 :label-fi             "Sakkopäätös / varsinainen päätös"
                 :label-sv             "Sakkopäätös / varsinainen päätös (sv)"
                 :valid                true
                 :manually-deliverable false
                 :allow-comments       false}
                {:id                   16
                 :label-fi             "Sakkopäätös / tiedoksianto (ensimmäinen postitus)"
                 :label-sv             "Sakkopäätös / tiedoksianto (ensimmäinen postitus) (sv)"
                 :valid                true
                 :manually-deliverable false
                 :allow-comments       false}
                {:id                   17
                 :label-fi             "Sakkopäätös / tiedoksianto (toinen postitus)"
                 :label-sv             "Sakkopäätös / tiedoksianto (toinen postitus) (sv)"
                 :valid                true
                 :manually-deliverable false
                 :allow-comments       false}
                {:id                   18
                 :label-fi             "Sakkopäätös / tiedoksianto (Haastemies)"
                 :label-sv             "Sakkopäätös / tiedoksianto (Haastemies) (sv)"
                 :valid                true
                 :manually-deliverable false
                 :allow-comments       false}
                {:id                   19
                 :label-fi             "Sakkopäätös / odotetaan valitusajan umpeutumista"
                 :label-sv             "Sakkopäätös / odotetaan valitusajan umpeutumista (sv)"
                 :valid                true
                 :manually-deliverable false
                 :allow-comments       false}
                {:id                   20
                 :label-fi             "Sakkopäätös / valitusaika umpeutunut"
                 :label-sv             "Sakkopäätös / valitusaika umpeutunut (sv)"
                 :valid                true
                 :manually-deliverable false
                 :allow-comments       false}
                {:id                   21
                 :label-fi             "Sakkoluettelon lähetys menossa"
                 :label-sv             "Sakkoluettelon lähetys menossa (sv)"
                 :valid                true
                 :manually-deliverable false
                 :allow-comments       false}])))

    (t/testing "Toimenpidetyypit matches the schema"
      (t/is (nil? (schema/check [valvonta-kaytto-schema/Toimenpidetyypit]
                                toimenpidetyypit))))))
