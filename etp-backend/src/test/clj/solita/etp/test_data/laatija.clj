(ns solita.etp.test-data.laatija
  (:require [solita.etp.test-system :as ts]
            [solita.etp.test-data.generators :as generators]
            [solita.etp.schema.laatija :as laatija-schema]
            [solita.etp.service.kayttaja-laatija :as kayttaja-laatija-service]))

(defn generate [n schema]
  (take n (map #(generators/complete {:henkilotunnus %1
                                      :email %2
                                      :patevyystaso (rand-nth [1 2])}
                                     schema)
               (generators/unique-henkilotunnukset n)
               (generators/unique-emails n))))

(defn generate-adds [n]
  (generate n laatija-schema/KayttajaLaatijaAdd))

(defn generate-updates [n]
  (generate n laatija-schema/KayttajaLaatijaUpdate))

(defn insert! [kayttaja-laatija-adds]
  (kayttaja-laatija-service/upsert-kayttaja-laatijat! ts/*db* kayttaja-laatija-adds))

(defn generate-and-insert! [n]
  (let [kayttaja-laatija-adds (generate-adds n)]
    (zipmap (insert! kayttaja-laatija-adds) kayttaja-laatija-adds)))
