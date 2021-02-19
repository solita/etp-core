(ns solita.etp.test-data.laatija
  (:require [solita.etp.test-system :as ts]
            [solita.etp.test-data.generators :as generators]
            [solita.etp.schema.laatija :as laatija-schema]
            [solita.etp.service.kayttaja-laatija :as kayttaja-laatija-service]))

(defn generate [n]
  (take n (map #(generators/complete {:henkilotunnus %1
                                      :email %2
                                      :patevyystaso (rand-nth [1 2])}
                                     laatija-schema/KayttajaLaatijaAdd)
               (generators/unique-henkilotunnukset n)
               (generators/unique-emails n))))

(defn insert! [laatijat]
  (kayttaja-laatija-service/upsert-kayttaja-laatijat! ts/*db* laatijat))

(defn generate-and-insert! [n]
  (insert! (generate n)))
