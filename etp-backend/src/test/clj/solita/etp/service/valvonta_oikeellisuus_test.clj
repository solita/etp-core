(ns solita.etp.service.valvonta-oikeellisuus-test
  (:require [clojure.test :as t]
            [solita.etp.test-system :as ts]
            [solita.etp.whoami :as test-whoami]
            [solita.etp.test-data.laatija :as laatija-test-data]
            [solita.etp.test-data.energiatodistus :as energiatodistus-test-data]
            [solita.etp.service.valvonta-oikeellisuus :as service]
            [solita.etp.test-data.kayttaja :as kayttaja-test-data]
            [solita.etp.test :as etp-test]))

(t/use-fixtures :each ts/fixture)

(t/deftest find-valvonta
  (let [paakayttaja-id (kayttaja-test-data/insert-paakayttaja!)
        [laatija-id _] (laatija-test-data/generate-and-insert!)
        [id _] (energiatodistus-test-data/generate-and-insert!
                 2018 true laatija-id)
        valvonta (service/find-valvonta (ts/db-user paakayttaja-id)
                                        (test-whoami/paakayttaja paakayttaja-id)
                                        id)]
    (t/is (some? valvonta))
    (t/is (= valvonta {:id id, :pending false, :valvoja-id nil, :ongoing false}))))

(t/deftest find-valvonta-laatija
  (let [[laatija-id _] (laatija-test-data/generate-and-insert!)
        [laatija2-id _] (laatija-test-data/generate-and-insert!)
        [id _] (energiatodistus-test-data/generate-and-insert!
                 2018 true laatija-id)
        valvonta (service/find-valvonta (ts/db-user laatija-id)
                                        (test-whoami/laatija laatija-id)
                                        id)]
    (t/is (some? valvonta))
    (t/is (= valvonta {:id id, :pending false, :valvoja-id nil, :ongoing false}))

    (t/is (= (etp-test/catch-ex-data
               #((service/find-valvonta (ts/db-user laatija2-id)
                                        (test-whoami/laatija laatija2-id)
                                        id)))
             {:type   :forbidden,
              :reason (str "User " laatija2-id
                           " is not allowed to access laatija: " laatija-id
                           " valvonta information.")}))))
