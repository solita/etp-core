(ns solita.etp.service.whoami-test
  (:require [clojure.test :as t]
            [schema.core :as schema]
            [schema-generators.generators :as g]
            [solita.etp.test-system :as ts]
            [solita.etp.service.whoami :as service]
            [solita.etp.service.kayttaja :as kayttaja-service]
            [solita.etp.schema.kayttaja :as kayttaja-schema]
            [solita.etp.schema.laatija :as laatija-schema]))

(t/use-fixtures :each ts/fixture)

(def paakayttaja {:rooli 2})

(t/deftest update-kayttaja-with-whoami!-test
  (doseq [kayttaja (repeatedly 100 #(g/generate laatija-schema/KayttajaAdd))
          :let [id (kayttaja-service/add-kayttaja! ts/*db* kayttaja)
                found-before (kayttaja-service/find-kayttaja ts/*db* paakayttaja id)
                new-email (str "new-" (:email found-before))
                cognitoid (str "cognitoid-" (rand-int 1000000))
                virtuid "tunnus"
                virtuorganisaatio "organisaatio"
                _ (service/update-kayttaja-with-whoami! ts/*db*
                                                        {:id id
                                                         :email new-email
                                                         :cognitoid cognitoid
                                                         :virtuid virtuid
                                                         :virtuorganisaatio virtuorganisaatio})
                found-after (kayttaja-service/find-kayttaja ts/*db* paakayttaja id)]]
    (schema/validate kayttaja-schema/Kayttaja found-after)
    (t/is (-> found-before :login nil?))
    (t/is (-> found-after :login nil? not))
    (t/is (-> found-after :email (= new-email)))
    (t/is (-> found-before :cognitoid nil?))
    (t/is (= cognitoid (:cognitoid found-after)))
    (t/is (-> found-before :virtuid nil?))
    (t/is (= virtuid (:virtuid found-after)))
    (t/is (-> found-before :virtuorganisaatio nil?))
    (t/is (= virtuorganisaatio (:virtuorganisaatio found-after)))))
