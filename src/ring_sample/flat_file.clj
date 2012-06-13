(ns ring-sample.flat-file
  (:require [clojure.string]))

(def data (ref []))

(defn read-data-from-file
  ""
  []
  (let [file-contents (slurp "/Users/diamond/projects/ring-sample/data/characters.txt")]
    (map #(clojure.string/split % #"\t")
         (clojure.string/split file-contents #"\n"))))

;;(read-data-from-file)

(defn init-data
  ""
  []
  (let [records (read-data-from-file)
        column-names (map keyword (first records))]
    (dosync (alter data into
                   (map #(zipmap column-names %) (rest records))))))

;;(init-data)

(defn add-record
  "Adds a new record to our data"
  [record]
  (dosync (alter data conj record)))

;; (add-record {:id "4" :name "Joe Louis"})

(defn- replace-record
  ""
  [current-data updated-record]
  (map #(if (= (:id %) (:id updated-record))
          updated-record
          %)
       current-data))

(defn update-record
  "Updates an existing record in our data"
  [record]
  (dosync (alter data replace-record record)))

;; (update-record {:id "4" :name "Ray Robinson"})
