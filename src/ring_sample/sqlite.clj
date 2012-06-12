(ns ring-sample.sqlite
  (:use [clojure.java.jdbc]))

(def db {:classname   "org.sqlite.JDBC"
         :subprotocol "sqlite"
         :subname     "data/characters.db"})

(defn make-table
  "Create the one table in our database."
  []
  (try (with-connection db
         (create-table :characters
                       [:id :integer]
                       [:name :text]
                       [:description :text]
                       [:birthday :datetime]))
       (catch Exception e (println e))))

(make-table)
