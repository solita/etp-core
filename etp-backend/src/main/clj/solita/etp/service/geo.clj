(ns solita.etp.service.geo
  (:require [solita.etp.schema.geo :as geo-schema]))

(def toimintaalueet [{:id 0 :label "Etelä-Karjala"}
                     {:id 1 :label "Etelä-Pohjanmaa"}
                     {:id 2 :label "Etelä-Savo"}
                     {:id 3 :label "Kainuu"}
                     {:id 4 :label "Kanta-Häme"}
                     {:id 5 :label "Keski-Pohjanmaa"}
                     {:id 6 :label "Keski-Suomi"}
                     {:id 7 :label "Kymenlaakso"}
                     {:id 8 :label "Lappi"}
                     {:id 9 :label "Pirkanmaa"}
                     {:id 10 :label "Pohjanmaa"}
                     {:id 11 :label "Pohjois-Karjala"}
                     {:id 12 :label "Pohjois-Pohjanmaa"}
                     {:id 13 :label "Pohjois-Savo"}
                     {:id 14 :label "Päijät-Häme"}
                     {:id 15 :label "Satakunta"}
                     {:id 16 :label "Uusimaa"}
                     {:id 17 :label "Varsinais-Suomi"}])

(defn find-toimintaalueet [] toimintaalueet)
