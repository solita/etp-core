(ns solita.etp.test-utils
  (:require [solita.etp.db]
            [schema-generators.generators :as g]
            [solita.etp.schema.common :as common-schema]
            [solita.etp.schema.laatija :as laatija-schema]
            [solita.etp.schema.geo :as geo-schema]))

(defn unique-henkilotunnus-range [to]
  (->> (range 0 to)
       (map (partial format "%09d"))
       (map #(str % (common-schema/henkilotunnus-checksum %)))
       (map #(str (subs % 0 6) "-" (subs % 6 10)))))

(def laatija-generators
  {common-schema/Henkilotunnus       (g/always "130200A892S")
   laatija-schema/MuutToimintaalueet (g/always [0, 1, 2, 3, 17])
   common-schema/Date                (g/always (java.time.LocalDate/now))
   geo-schema/Maa                    (g/always "FI")})


(defn generate-kayttaja [n schema]
  (map #(assoc %1
               :email (str %2 "@example.com")
               :henkilotunnus %3)
       (repeatedly n #(g/generate schema laatija-generators))
       (repeatedly n #(.toString (java.util.UUID/randomUUID)))
       (unique-henkilotunnus-range n)))
