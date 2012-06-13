(ns ring-sample.sqlite
  (:require [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]))

(def db {:classname   "org.sqlite.JDBC"
         :subprotocol "sqlite"
         :subname     "data/characters.db"})

(defn make-table
  "Create the one table in our database."
  []
  (try (jdbc/with-connection db
         (jdbc/create-table :characters
                       [:id :integer]
                       [:name :text]
                       [:description :text]
                       [:birthday :datetime]))
       (catch Exception e (println e))))

;;(make-table)


;; This executes a query against our SQLite database.
;;
;; with-connection db opens a connection to the database, and binds
;; that connection to a var inside of the Clojure JDBC namespaces.
;; The binding is visible only within the scope of this with-connection
;; statement.
;;
;; Another thread may be executing simultaneously, and it may bind
;; the connection var inside the Clojure JDBC namespace to something
;; else. We can't see that binding from within the scope of our function,
;; and the other function cannot see our connection binding from within
;; its scope.
;;
;; query-with-results executes the SQL query and stores the result in
;; the var rs. This result is like a cursor object in Python, or a
;; statement handle in the Perl and Ruby DBI frameworks. It's just
;; pointing to a set of rows.
;;
;; When we call into [], we are iterating through all the rows in the
;; result set and adding them all into a vector. [] is an empty vector.
;;
;; Clojure JDBC returns a hash for each row. The hash keys are column
;; names, but they are keywords instead of strings. So a row from our
;; database would look like this:
;;
;; {:id 1, :name "Bugs Bunny" :description "Wascally Wabbit"
;;  :birthday "1936-10-10"}
;;
;; Note that Clojure supports throw in addition to try, catch and finally.
;;
(defn query
  "Executes a query. Returns a vector of results. Each item in the vector
   is a hash, keyed by column name. Param sql must be a vector. To execute
   a simple SQL string, pass in a vector containing only the string. To
   execute a query or statement with params, the sql string should come first,
   followed by the params to be bound."
  [sql]
  (try
    (jdbc/with-connection db
      (jdbc/with-query-results rs sql
        (into [] rs)))
    (catch Throwable t (prn sql) (throw t))))


;; This function calls query and converts the result to JSON
;;
(defn query-json
  "Executes a query and returns the result as json. Param sql should be a
   vector with [sql-string params...] or just [sql-string]."
  [sql]
    (json/generate-string (query sql)))


;; This is a ring request handler. It takes a single parameter,
;; the request object, and returns a string. We could return a
;; hash with keys :headers, :status, and :body, but when we return
;; a simple string, ring fills in the necessary headers, and
;; generates a 200 response.
;;
;; In core.clj we defined this as the handler for GET /sqlite
;;
(defn show-index
  "Returns a list of records from our SQLite database."
  [request]
  (query-json ["select * from characters order by name"]))
