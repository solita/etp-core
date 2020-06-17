(ns solita.etp.service.energiatodistus-xlsx
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [solita.common.map :as solita-map]
            [solita.common.xlsx :as xlsx]
            [solita.etp.service.energiatodistus :as energiatodistus-service]))

(def tmp-dir "tmp/")

(def col-order [:id :versio :laatija-id :laatija-fullname
                :allekirjoitusaika :korvattu-energiatodistus-id :perustiedot
                :lahtotiedot :tulokset :toteutunut-ostoenergiankulutus :huomiot
                :lisamerkintoja-fi :lisamerkintoja-sv])

(defn paths-for-k [energiatodistukset k]
  (->> energiatodistukset
       (map #(-> % (select-keys [k]) solita-map/paths))
       (apply concat)
       (into (sorted-set))))

(defn other-paths [energiatodistukset]
  (->> energiatodistukset
       (map #(-> (apply dissoc % col-order) solita-map/paths))
       (apply concat)
       (into (sorted-set))))

(defn path->str [path]
  (->> path
       (map #(if (keyword %) (name %) %))
       (map str/capitalize)
       (str/join " / ")))

(defn col-width [label]
  (max 2000 (* (count label) 250)))

(defn fill-headers [sheet style paths]
  (let [row (xlsx/create-row sheet 0)]
    (.setRowStyle row style)
    (doseq [[idx path] (map-indexed vector paths)
            :let [label (path->str path)]]
      (xlsx/create-cell-with-value row idx label)
      (xlsx/set-column-width sheet idx (col-width label)))))

(defn fill-row-with-energiatodistus [sheet idx energiatodistus paths date-style]
  (let [row (xlsx/create-row sheet idx)]
    (doseq [[idx path] (map-indexed vector paths)
            :let [v (get-in energiatodistus path)
                  cell (xlsx/create-cell-with-value row idx v)]]
      (if (instance? java.time.LocalDate v)
        (.setCellStyle cell date-style)))))

(defn find-laatija-energiatodistukset-xlsx [db laatija-id]
  (when-let [energiatodistukset
             (energiatodistus-service/find-complete-energiatodistukset-by-laatija
              db
              laatija-id
              nil)]
    (let [file-path (->> (java.util.UUID/randomUUID)
                    .toString
                    (format "energiatodistus-%s.xlsx")
                    (str tmp-dir))
          paths (concat (mapcat #(paths-for-k energiatodistukset %) col-order)
                        (other-paths energiatodistukset))
          xlsx (xlsx/create-xlsx)
          sheet (xlsx/create-sheet xlsx "Energiatodistukset")
          bold-font (xlsx/create-bold-font xlsx)
          bold-style (xlsx/create-style-with-font xlsx bold-font)
          date-style (xlsx/create-style-with-format xlsx "d.mm.yyyy")]
      (fill-headers sheet bold-style paths)
      (doseq [[idx energiatodistus] (map-indexed vector energiatodistukset)]
        (fill-row-with-energiatodistus sheet (inc idx) energiatodistus paths date-style))
      (io/make-parents file-path)
      (xlsx/save-xlsx xlsx file-path)
      (let [is (io/input-stream file-path)]
        (io/delete-file file-path)
        is))))
