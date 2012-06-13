(ns ring-sample.sqlite
  (:require [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
            [ring-sample.util :as util]))

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


;; The & in the parameter list says that the function can take one or more
;; optional parameters.
;;
(defn execute-sql
  "Executes a sql statement. Param sql is a sql string and the following args
   are seqs of parameters to be bound to the sql statement."
  [sql & params]
  (try
    (jdbc/with-connection db
      (jdbc/do-prepared sql params))
    (catch Throwable t (prn sql) (throw t))))


(defn init-db!
  "Initializes the SQLite database with a table and some records."
  []
  (println "Initializing SQLite database")
  (make-table)
  (jdbc/with-connection db
    (jdbc/insert-records :characters
                         {:id 1 :name "Fred Flintstone"
                          :description "Stone-age Ralph Kramden"
                          :birthday "1939-03-08"}
                         {:id 2 :name "Wilma Flintstone"
                          :description "Wife of Fred"
                          :birthday "1939-06-22"}
                         {:id 3 :name "Fred Astair"
                          :description "Actor, dancer"
                          :birthday "1912-01-16"})))


;; This is the SQL query we'll execute.
(def sql "select * from characters order by name")


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
  (if (empty? (query [sql]))
    (init-db!))
  (query-json [sql]))


;; This tells util/get-params how to convert HTTP params, which are
;; all strings, to the proper data types.
;;
(def param-config
     [{:name :id           :type :int       :default nil}
      {:name :name         :type :string    :default nil}
      {:name :description  :type :string    :default nil}
      {:name :birthday     :type :datetime  :default nil} ])


;; Notice the str function, which concatenates as many strings as you
;; give it. If one of the params is not a string, str calls Java's
;; toString() method on it.
;;
;; If this call succeeds, it returns the ring request object as JSON,
;; with the typed-params stuffed into it. We hack the record id into
;; the JSON so you'll see that it was converted to an integer by
;; util/get-params. (The id_hacked_in property of the JSON will not be
;; quoted. That tells you it's an integer.)
;;
;; At the end of the function, we use dissoc to remove the body from
;; the request hash, because it's a type of object the serializer cannot
;; serialize to JSON. dissoc returns a copy of the original hash with
;; the dissoc'ed item removed.
;;
;; We then merge our new copy of the request hash with another hash
;; containing item(s) we want to add. Then the whole thing is serialized
;; to JSON.
;;
;; If the call fails, it returns an error message in JSON format.
;;
(defn create
  "Creates a new record. This is the ring handler for POST /sqlite/"
  [request]
  (let [typed-params (util/get-params (:params request) param-config)
        insert-statement (str "insert into characters "
                              "(id, name, description, birthday) "
                              "values (?, ?, ?, ?)")]
    (try (execute-sql insert-statement
                      (:id typed-params)
                      (:name typed-params)
                      (:description typed-params)
                      (:birthday typed-params))
         (catch Exception ex
           (json/generate-string {:error (.getMessage ex)})))
    (json/generate-string (merge (dissoc request :body)
                                 {:id_converted_to_int (:id typed-params)}))))

(defn update
  "Updates a record. This is the ring handler for POST /sqlite/:id"
  [request]
  (let [typed-params (util/get-params (:params request) param-config)
        insert-statement (str "update characters "
                              "set name=?, description=?, birthday=? "
                              "where id=?")]
    (try (execute-sql insert-statement
                      (:name typed-params)
                      (:description typed-params)
                      (:birthday typed-params)
                      (:id typed-params))
         (catch Exception ex
           (json/generate-string {:error (.getMessage ex)})))
    (json/generate-string (merge (dissoc request :body)
                                 {:id_converted_to_int (:id typed-params)}))))
