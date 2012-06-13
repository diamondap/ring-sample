(ns ring-sample.util

  ;; This use statement imports a single function
  ;; called blank? from the clojure.string namespace
  ;; into the current namespace.

  (:use [clojure.string :only [blank?]])

  (:require [cheshire.core :as json])

  ;; :import makes some Java classes accessible within
  ;; the current namespace. The first line after :import
  ;; makes the 7 named classes from org.joda.time accessible.
  ;; All of the :import items are pure Java, and all can
  ;; be easily accessed from Clojure.

  (:import
   (org.joda.time DateTime Days Seconds DateTimeZone Period Interval Duration)
   (org.joda.time.format DateTimeFormat)
   (org.joda.time.tz UTCProvider)))

;; ---------------------------------------------------------------------------
;; Numeric Conversion
;; ---------------------------------------------------------------------------


;; Following are two utility functions for converting strings that
;; come in through the ring request hash into proper numeric types.
;; These functions use Java classes to do some of their work.
;;
;; Note that static Java methods are accessed just like
;; Clojure functions. In Clojure, you call namespace/function.
;; When calling a Java static method, you call classname/method.
;;
;; The result of (Double/parseDouble str) is a Java Double object.
;; We then call intvalue on that to get the integer portion.
;;
;; So this Clojure:   (.intValue (Double/parseDouble str)
;;
;; Is equivalent to this Java:  Double.parseDouble(str).intValue()
;;
;; The dot notation in .intValue says "call the intValue method
;; on the following parameter."
;;
;; Note that this function also uses try and catch. Clojure
;; supports finally as well. Also note that the catch is within
;; the try form.
;;
(defn to-i
  "Converts str to int. Returns null if input is null or conversion fails.
   Values with decimals will be trucated, so 7.75 becomes 7."
  [str]
  (if str
    (try (.intValue (Double/parseDouble str))
         (catch Throwable t nil))))

(defn to-f
  "Converts str to double-precision float. Returns null if input is null or
   conversion fails."
  [str]
  (if str
    (try (Double/parseDouble str)
         (catch Throwable t nil))))

;; ---------------------------------------------------------------------------
;; DateTime Utilities
;; ---------------------------------------------------------------------------

;; The next few defs are datetime formats that the Joda datetime
;; library can use for parsing and for formatting.
;;
(def db-datetime "YYYY-MM-dd'T'HH:mm:ssZZ")
(def yyyymmdd-dash "YYYY-MM-dd")
(def yyyymmdd-slash "YYYY/MM/dd")
(def mmddyyyy-slash "MM/dd/YYYY")

;; The next few defs are regular expressions that our program uses
;; to try to identify paramers that look like dates and datetimes.
;;
;; The function re-pattern converts a regular expression pattern
;; string into a Java RegularExpression object. Clojure's core has
;; strong support for regular expressions and pattern matching.
;;
;; The shorthand #"xyz" (pound sign followed by a quoted string)
;; is the same as (re-pattern "xyz")
;;
(def re-db-datetime
     (re-pattern
      "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}$"))
(def re-yyyymmdd-dash #"^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
(def re-yyyymmdd-slash #"^[0-9]{4}/[0-9]{2}/[0-9]{2}$")
(def re-mmddyyyy-slash #"^[0-9]{2}/[0-9]{2}/[0-9]{4}$")


;; This function uses cond to test a series of expressions.
;; cond will return the value to the right of the first expression
;; that evaluates to true. If no expression is true, it will return
;; the value to the right of :else.
;;
(defn guess-dt-format [dt-str]
  "Tries to figure out the format of a datetime string. Returns the format,
   if it can figure it out, or nil if it cannot."
  (if (string? dt-str)
    (cond
     (blank? dt-str) nil
     (re-find re-db-datetime dt-str) db-datetime
     (re-find re-yyyymmdd-dash dt-str) yyyymmdd-dash
     (re-find re-yyyymmdd-slash dt-str) yyyymmdd-slash
     (re-find re-mmddyyyy-slash dt-str) mmddyyyy-slash
     :else nil)))


;; This function shows Clojure's equivalent of method overloading.
;; The defn actually defines two functions with the name datetime-str.
;; The first takes a single parameter called pattern. The second takes
;; two parameters: a datetime and a pattern. The first form of the
;; function simply calls the second form with the current time as the
;; dt parameter.
;;
(defn datetime-str
  "Returns a timestamp in the form of a string with the specified pattern."
  ([pattern]
     (datetime-str (new DateTime) pattern))
  ([dt pattern]
     (.(. DateTimeFormat forPattern pattern) print (. dt toDateTime))))

(defn parse-datetime
  "Parses a string, returns a DateTime object."
  [dt-str pattern]
  (let [fmt (. DateTimeFormat forPattern pattern)]
    (. fmt parseDateTime dt-str)))

(defn try-parse-datetime
  "Tries to guess the format of a datetime string and parse it."
  [dt-str]
  (let [pattern (guess-dt-format dt-str)]
    (if pattern
      (try (parse-datetime dt-str pattern)
           (catch Throwable t nil)))))

;; We can create an instance of a Java object by calling
;; (new <ClassName>). If the constructor requires arguments,
;; we can call (new <ClassName> arg1 arg2 etc)
;;
(defn now
  "Returns a new DateTime set to the current date and time."
  []
  (new DateTime))


;; ---------------------------------------------------------------------------
;; Query Param Utilities
;; ---------------------------------------------------------------------------


(defn date-param
  "Returns the value of the param with the specified key as a float,
   or default if the value is null or empty. param-name should be symbol."
  [date-str default]
  (let [datetime (if date-str (try-parse-datetime date-str))]
    (if datetime
      datetime
      (try-parse-datetime default))))

(defn get-param
  "Returns param from params map, cast to type. Returns default if the value
   is missing or empty."
  [params param-name type default]
  (if (blank? (param-name params))
    (if (= :datetime type)
      (try-parse-datetime default)
      default)
    (let [value (param-name params)]
      (case type
            :int (to-i value)
            :float (to-f value)
            :datetime (date-param value default)
            value))))

;; This function takes a map (a.k.a. a hash) of parameters from
;; the ring request and converts each parameter to whatever type
;; the config map specifies.
;;
;; Here's what's happening line by line:
;;
;; * (let [param-names (map :name config)
;;   We call the keyword :name, as if it were a function, on each
;;   item in config. config is a vector of hashes. When you call
;;   a keyword on a hash, you get the value associated with that
;;   keyword within the hash (or nil if hash does not contain that
;;   keyword as a key). The result of calling :name on config
;;   (whose structure you can see in the doc string below) is a
;;   lazy sequence of all the names of all the configuration items.
;;
;; * values (map #(get-param params (:name %1) (:type %1) (:default %1))
;;                    config)]
;;   This is also part of the let form, creating a local var. Again we
;;   call map, which invokes a function on every item the second param.
;;   Here, the second param is config. The first param to map is the
;;   function we want to invoke. In this case, it's an anonymous
;;   function, defined by the shorthand #(). The body of the anonymous
;;   function goes inside the parentheses. Here, we are calling get-param,
;;   passing in the params hash, which has our HTTP request params.
;;   We also pass get-param the param name, type and default value
;;   specified in the config. %1 is a placeholder for the parameter
;;   that gets pulled out of config.
;;
;;   Remember that config is a vector of hashes. When we call map,
;;   we are invoking our function on each item in the vector. Each item
;;   is a hash. So (:name %1) pulls the name out of the hash that was
;;   passed into our anonymous function. (:type %1) pulls out the type,
;;   and (:default %1) pulls out the default value.
;;
;;   If this call to map were operating on the config values shown
;;   in the docstring below, we would be executing:
;;
;;   (get-param params :company :string "Apple")
;;   (get-param params :age :int 26)
;;   (get-param params :weight :float 188.6)
;;   ...etc.
;;
;;   The results of all these function calls are gathered into a
;;   lazy sequence and associated with the var "values". Since get-param
;;   just converts strings into numbers, dates, and other strongly-typed
;;   values, the "values" var will contain a lazy sequence of objects
;;   of various types.
;;
;;   Finally, this fuction calls zip-map on the param-names and values.
;;   zip-map creates a map (hahs) from two sequences. The first item
;;   in the first sequence becomes the first key, and first item in the
;;   second sequence becomes the value associated with that key. All
;;   of the keys and values are associated in order.
;;
;;   Notice that in the let statement, we created two sequences, and
;;   that they will be in the same order. The hash that zip-map builds
;;   would look like this, given the config vector in the docstring:
;;
;;   {:company "Apple", :age 26, :weight 188, ...etc.}
;;
;;   A couple of things to note here are:
;;
;;   1. The return value of a function is the value of the last thing
;;      that was evaluated inside the function.
;;
;;   2. The call to zip-map is inside of the let form. When let creates
;;      local vars, they only exist until you reach the parenthesis
;;      that ends the let form.
;;
(defn get-params
  "Given a map of HTTP input params (in which keys are symbols), and a
   seq of config maps describing data types and default values, returns a
   map of params with values cast to the correct type. Param config looks
   like this:

   [{:name :company, :type :string, :default 'Apple'},
    {:name :age, :type :int, :default 26},
    {:name :weight, :type :float, :default 188.6},
    {:name :date_of_birth, :type :datetime, :default '02/21/1916'},
    {:name :something, :type :int, :default nil}]
  "
  [params config]
  (let [param-names (map :name config)
        values (map #(get-param params (:name %1) (:type %1) (:default %1))
                    config)]
    (zipmap param-names values)))


;; This function just lets us send the ring request hash back to the
;; browser so curious programmers can have a look at it.
;;
;; We call dissoc on the request hash to remove the body, which is an
;; instance of a Java HttpInputStream and cannot be serialized to JSON.
;;
;; All of the items in the request body will show up in the params hash,
;; because we are using the ring wrap-params middleware. So you'll still
;; be able to see that data in the JSON.
;;
(defn dump-request
  "Dump the ring request hash back to the client."
  [request]
  (json/generate-string (dissoc request :body)))

