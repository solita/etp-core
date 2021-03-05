(ns solita.etp.test-data.energiatodistus
  (:require [schema.core :as schema]
            [schema-generators.generators :as g]
            [flathead.deep :as deep]
            [solita.common.logic :as logic]
            [solita.common.schema :as xschema]
            [solita.etp.test-system :as ts]
            [solita.etp.test-data.generators :as generators]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.energiatodistus :as energiatodistus-schema]
            [solita.etp.service.energiatodistus :as energiatodistus-service]))

(def generators {schema/Num                   (g/always 1.0M)
                 common-schema/NonNegative    (g/always 1.0M)
                 common-schema/IntNonNegative (g/always 1)})

(defn schema-by-version-and-ready-for-signing [versio ready-for-signing?]
  (cond->> (if (= versio 2013)
             energiatodistus-schema/EnergiatodistusSave2013
             energiatodistus-schema/EnergiatodistusSave2018)
    ready-for-signing? (deep/map-values record?
                                        (logic/when* xschema/maybe? :schema))))

(defn sisainen-kuorma [versio id]
  (-> (energiatodistus-service/find-sisaiset-kuormat ts/*db* versio)
      (->> (filter (comp (partial = id) :kayttotarkoitusluokka-id)))
      first
      (dissoc :kayttotarkoitusluokka-id)))

(defn generate-adds [n versio ready-for-signing?]
  (repeatedly n #(generators/complete
                  {:perustiedot (merge
                                 {:kieli (rand-int 2)
                                  :kayttotarkoitus "YAT"}
                                 (if (= versio 2018)
                                   {:laatimisvaihe (rand-int 2)}))
                   :lahtotiedot {:ilmanvaihto {:tyyppi-id (rand-int 7)}
                                 :lammitys {:lammitysmuoto-1 {:id (rand-int 10)}
                                            :lammitysmuoto-2 {:id (rand-int 10)}
                                            :lammonjako {:id (rand-int 13)}}
                                 :sis-kuorma (sisainen-kuorma versio 1)}
                   :laskutettava-yritys-id nil
                   :korvattu-energiatodistus-id nil}
                  (schema-by-version-and-ready-for-signing versio
                                                           ready-for-signing?)
                  generators)))

(def generate-updates generate-adds)

(defn insert! [energiatodistus-adds laatija-id]
  (mapv #(:id (energiatodistus-service/add-energiatodistus!
               (ts/db-user laatija-id)
               {:id laatija-id}
               (if (-> % :perustiedot (contains? :uudisrakennus))
                 2013
                 2018)
               %))
        energiatodistus-adds))

(defn generate-and-insert! [n versio ready-for-signing? laatija-id]
  (let [energiatodistus-adds (generate-adds n versio ready-for-signing?)]
    (zipmap (insert! energiatodistus-adds laatija-id) energiatodistus-adds)))
