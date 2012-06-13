(ns ring-sample.flat-file
  (:require [clojure.string]
            [cheshire.core :as json]
            [ring-sample.util :as util]))

;; We're going to read tab-delimited data from a flat text file,
;; then allow that data to be updated from submitted web forms.
;;
;; Because our server may be running several threads at once, there
;; may be several threads trying to update the data at the same time.
;; The data is a shared resource, so let's declare it as a ref, which
;; can only be updated inside a dosync transaction.
;;
;; We'll start out by defining a ref called data. Its value is an empty
;; vector.
;;
(def data (ref []))

;; This is our data file
;;
(def file-path "/Users/diamond/projects/ring-sample/data/characters.txt")


;; This function parses the data from the file.
;; This uses slurp to read the entire contents of the file into
;; a string. Then it splits the string on newlines to create
;; a sequence of records. It splits each record on tabs to create
;; fields within each record. The result is sequence of records,
;; with each record containing a sequence of fields.
(defn read-data-from-file
  "Reads data from the flat file and parses it into a series of
   vectors-- one vector for each record. The first record has
   the column names."
  []
  (let [file-contents (slurp file-path)]
    (map #(clojure.string/split % #"\t")
         (clojure.string/split file-contents #"\n"))))


;; This function reads all of the data from the file into a local
;; var called records, which is a vector of vectors. Since the first
;; line of the file becomes the first vector, and that line contains
;; the column names, we can get a vector of column names by calling
;; (first records). We call the keyword function on each of the column
;; names so that column-names ends up being a sequence of keywords.
;;
;; Then we alter the value of our data ref. We have to do this inside
;; of a synchronous transaction. That's the only way to update a ref.
;; dosync locks the var so other threads can't alter it. It then tries
;; to execute whatever code is inside the dosync parentheses. If it
;; works, the value of data is updated. If Clojure can't get the lock
;; because some other thread has locked this var, Clojure will continue
;; to retry until it can get the lock and perform the update.
;;
;; If anything fails inside the dosync, any pending changes will be
;; rolled back without any external code ever seeing the var in an
;; inconsistent state. This is similar to an atomic database transaction,
;; where changes are not visible until the commit occurs.
;;
;; The result of this fuction is that data will contain a vector of
;; hashes. Each hash has keyword keys (:id, :name, etc.) and
;; string values, all from the data file.
;;
;; This function ends with an exclamation point because it has side-effects.
;; It alters the state of the data ref. By convention, functions whose
;; primary purpose to cause some side effect, rather than to return a
;; value, end with an exclamation point.
;;
;;
(defn init-data!
  "Initializes the data ref with the records from the data file."
  []
  (println "Initializing data")
  (let [records (read-data-from-file)
        column-names (map keyword (first records))]
    (dosync (alter data into
                   (map #(zipmap column-names %) (rest records))))))


;; This adds a new record to our data. As above, the change to data must
;; be done synchronously, with dosync acquiring a lock.
;;
;; The alter function takes three or more params: the first is the ref that
;; you want to alter, the second is a function that will alter the ref, and
;; the remaining params are additional parameters to be passed into the
;; function that will do the altering.
;;
;; In the code below, alter winds up calling:
;;
;; (apply conj data record)
;;
;; conj adds one or more items to a sequece. Here, it adds the new record
;; to the existing data vector.
;;
;; The ref will be set to the return value of (apply conj data record)
;;
(defn add-record
  "Adds a new record to our data"
  [record]
  (dosync (alter data conj record)))


;; This function replaces an existing record with an updated record.
;; Note that the defn- has a little dash attached to the end. That makes
;; this function private to the current namespace.
;;
(defn- replace-record
  ""
  [current-data updated-record]
  (map #(if (= (:id %) (:id updated-record))
          updated-record
          %)
       current-data))


;; This function updates a record in data by calling replace-record.
(defn update-record
  "Updates an existing record in our data"
  [record]
  (dosync (alter data replace-record record)))



;; Here we return the data in our data ref as a string of JSON.
;; Note that this function uses both notations for dereferencing the
;; data ref. If you just access data, you will get an instance of a
;; Java Ref object. To get the actual value contained in the reference,
;; you have to derefernce it with one of these equivalent syntaxes:
;;
;; (deref data)
;; @data
;;
;;
(defn show-index
  "This is the ring handler for GET /flat-file"
  [request]
  (if (empty? (deref data))
    (init-data!))
  (json/generate-string @data))


(defn update
  "This is the ring handler for POST /flat-file/:id"
  [request]
  (util/dump-request request))

(defn create
  "This is the ring handler for POST /flat-file"
  [request]
  (util/dump-request request))
