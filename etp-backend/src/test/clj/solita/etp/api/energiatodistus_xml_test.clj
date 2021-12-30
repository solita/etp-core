(ns solita.etp.api.energiatodistus-xml-test
  (:require [clojure.test :as t]
            [clojure.java.io :as io]
            [schema.core :as schema]
            [schema-tools.coerce :as sc]
            [solita.common.xml :as xml]
            [solita.common.xml-test :as xml-test]
            [solita.etp.api.energiatodistus-xml :as xml-api]))

(t/deftest xml->energiatodistus-test
  (let [energiatodistus (xml-api/xml->energiatodistus xml-test/xml-without-soap-envelope)]
    (t/is (= "Kilpikonna" (get-in energiatodistus [:perustiedot :nimi])))
    (t/is (= 0.5 (get-in energiatodistus [:lahtotiedot :rakennusvaippa :ikkunat :U])))
    (t/is (= 14.0 (get-in energiatodistus [:lahtotiedot :sis-kuorma :valaistus :lampokuorma])))
    (t/is (= 0.5 (get-in energiatodistus [:tulokset :tekniset-jarjestelmat :jaahdytys :kaukojaahdytys])))
    (t/is (empty? (get-in energiatodistus [:toteutunut-ostoenergiankulutus :muu])))
    (t/is (= "p:Nimi" (get-in energiatodistus [:huomiot :lammitys :toimenpide 0 :nimi-fi])))))
