(ns solita.etp.service.geo
  (:require [solita.etp.schema.geo :as geo-schema]))

(def maakunnat [{:id 0 :label "Ahvenanmaa"}
                {:id 1 :label "Etelä-Karjala"}
                {:id 2 :label "Etelä-Pohjanmaa"}
                {:id 3 :label "Etelä-Savo"}
                {:id 4 :label "Kainuu"}
                {:id 5 :label "Kanta-Häme"}
                {:id 6 :label "Keski-Pohjanmaa"}
                {:id 7 :label "Keski-Suomi"}
                {:id 8 :label "Kymenlaakso"}
                {:id 9 :label "Lappi"}
                {:id 10 :label "Pirkanmaa"}
                {:id 11 :label "Pohjanmaa"}
                {:id 12 :label "Pohjois-Karjala"}
                {:id 13 :label "Pohjois-Pohjanmaa"}
                {:id 14 :label "Pohjois-Savo"}
                {:id 15 :label "Päijät-Häme"}
                {:id 16 :label "Satakunta"}
                {:id 17 :label "Uusimaa"}
                {:id 18 :label "Varsinais-Suomi"}])

(defn find-maakunnat [] maakunnat)
