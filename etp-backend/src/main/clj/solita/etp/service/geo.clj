(ns solita.etp.service.geo
  (:require [solita.etp.db :as db]
            [solita.etp.service.json :as json]
            [solita.etp.schema.geo :as geo-schema]
            [schema.coerce :as coerce]))

; *** Require sql functions ***
(db/require-queries 'geo)

; *** Conversions from database data types ***
(def coerce-country (coerce/coercer geo-schema/Country json/json-coercions))

(defn find-all-countries [db]
  (map coerce-country (geo-db/select-countries db)))

(def toimintaalueet [{:id 0
                      :label-fi "Etelä-Karjala"
                      :label-se "Södra Karelen"}
                     {:id 1
                      :label-fi "Etelä-Pohjanmaa"
                      :label-se "Södra Österbotten"}
                     {:id 2
                      :label-fi "Etelä-Savo"
                      :label-se "Södra Savolax"}
                     {:id 3
                      :label-fi "Kainuu"
                      :label-se "Kajanaland"}
                     {:id 4
                      :label-fi "Kanta-Häme"
                      :label-se "Egentliga Tavastland"}
                     {:id 5
                      :label-fi "Keski-Pohjanmaa"
                      :label-se "Mellersta Österbotten"}
                     {:id 6
                      :label-fi "Keski-Suomi"
                      :label-se "Mellersta Finland"}
                     {:id 7
                      :label-fi "Kymenlaakso"
                      :label-se "Kymmenedalen"}
                     {:id 8
                      :label-fi "Lappi"
                      :label-se  "Lappland"}
                     {:id 9
                      :label-fi "Pirkanmaa"
                      :label-se "Birkaland"}
                     {:id 10
                      :label-fi "Pohjanmaa"
                      :label-se "Österbotten"}
                     {:id 11
                      :label-fi "Pohjois-Karjala"
                      :label-se "Norra Karelen"}
                     {:id 12
                      :label-fi "Pohjois-Pohjanmaa"
                      :label-se "Norra Österbotten"}
                     {:id 13
                      :label-fi "Pohjois-Savo"
                      :label-se "Norra Savolax"}
                     {:id 14
                      :label-fi "Päijät-Häme"
                      :label-se "Päijänne-Tavastland"}
                     {:id 15
                      :label-fi "Satakunta"
                      :label-se "Satakunta"}
                     {:id 16
                      :label-fi "Uusimaa"
                      :label-se "Nyland"}
                     {:id 17
                      :label-fi "Varsinais-Suomi"
                      :label-se "Egentliga Finland"}])

(defn find-toimintaalueet [] toimintaalueet)
