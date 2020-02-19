(ns solita.etp.service.geo
  (:require [solita.etp.schema.geo :as geo-schema]))

(def toimintaalueet [{:id 0
                      :label-fi "Etelä-Karjala"
                      :label-swe "Södra Karelen"}
                     {:id 1
                      :label-fi "Etelä-Pohjanmaa"
                      :label-swe "Södra Österbotten"}
                     {:id 2
                      :label-fi "Etelä-Savo"
                      :label-swe "Södra Savolax"}
                     {:id 3
                      :label-fi "Kainuu"
                      :label-swe "Kajanaland"}
                     {:id 4
                      :label-fi "Kanta-Häme"
                      :label-swe "Egentliga Tavastland"}
                     {:id 5
                      :label-fi "Keski-Pohjanmaa"
                      :label-swe "Mellersta Österbotten"}
                     {:id 6
                      :label-fi "Keski-Suomi"
                      :label-swe "Mellersta Finland"}
                     {:id 7
                      :label-fi "Kymenlaakso"
                      :label-swe "Kymmenedalen"}
                     {:id 8
                      :label-fi "Lappi"
                      :label-swe  "Lappland"}
                     {:id 9
                      :label-fi "Pirkanmaa"
                      :label-swe "Birkaland"}
                     {:id 10
                      :label-fi "Pohjanmaa"
                      :label-swe "Österbotten"}
                     {:id 11
                      :label-fi "Pohjois-Karjala"
                      :label-swe "Norra Karelen"}
                     {:id 12
                      :label-fi "Pohjois-Pohjanmaa"
                      :label-swe "Norra Österbotten"}
                     {:id 13
                      :label-fi "Pohjois-Savo"
                      :label-swe "Norra Savolax"}
                     {:id 14
                      :label-fi "Päijät-Häme"
                      :label-swe "Päijänne-Tavastland"}
                     {:id 15
                      :label-fi "Satakunta"
                      :label-swe "Satakunta"}
                     {:id 16
                      :label-fi "Uusimaa"
                      :label-swe "Nyland"}
                     {:id 17
                      :label-fi "Varsinais-Suomi"
                      :label-swe "Egentliga Finland"}])

(defn find-toimintaalueet [] toimintaalueet)
