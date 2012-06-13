(ns ring-sample.core

  ;; We start a clojure file with a namespace,
  ;; which groups all of the functions and vars
  ;; in the file under the specified namespace.
  ;; This prevents function and var definitions
  ;; from conflicting.

  ;; The namespace declaration typically has
  ;; :use and :require statements.

  ;; :use brings all of the functions and vars
  ;; from the used namespace into the current
  ;; namespace. You can also tell clojure to
  ;; import only certain items by saying this:
  ;; (:use [clojure.java.jdbc] :only (with-connection create-table))

  (:use [compojure.core]
        [ring.util.response]
        [ring.middleware.session]
        [ring.middleware.session.cookie]
        [ring.middleware.content-type]
        [ring.middleware.params])

  ;; :require allows you to define a prefix or alias
  ;; for a namespace, so that you can import its
  ;; functions and vars without conflicts.
  ;; Here, we require cheshire.core, and give it
  ;; the alias json since it provides json
  ;; serialization and deserialization functions.
  ;; When we call its functions below, we call them
  ;; with the prefix we've defined. For example,
  ;; json/generate-string.

  (:require [clojure.tools.logging :as log]
            [ring.adapter.jetty :as jetty]
            [cheshire.core :as json]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.string :as string]
            [ring-sample.sqlite :as sqlite]
            [ring-sample.flat-file :as flat-file]
            [ring-sample.util :as util])

  ;; Since this is the core of our application, and
  ;; we would like to be able to compile it into
  ;; a stand-alone application, we tell leiningen
  ;; to generate a Java class from this file, and
  ;; we specify :main true to indicate that this
  ;; namespace contains application's main method.
  ;; Main is special because it must be public,
  ;; static, and accept args. See -main below.

  (:gen-class :main true))  ;; end of ns declaration



;;
;; The function defroutes comes from compojure. When we said
;; to :use compojure above, we imported all of its functions.
;; When we define routes, we give compojure the HTTP verb,
;; the path, and a function to handle requests with that verb
;; to that path. Paths may include captures, such as
;; /employee/:id. The :id will show up in the request params.
;; When compojure calls one of the functions we defined, it
;; passes a request object, which you can inspect using the
;; dump function above.
;;
;; Handler functions should return either a hash with 3 keys
;; (:status, :headers: and :body), or a string. If they return
;; a string, that implies status code 200.
;;
(defroutes main-routes

  (GET "/sqlite" [] sqlite/show-index)
  (GET "/flat-file" [] flat-file/show-index)

  ;; Provide a route to which we can submit forms, and have
  ;; the server dump out the request.
  (POST "/dump" [] util/dump-request)

  ;; Anything not matching the routes above will match
  ;; the / route. Treat requests not matching routes above
  ;; as requests for static resources. These come out of
  ;; the resources/public directory.
  (route/resources "/")

  ;; If we got a request that did not match any of the
  ;; routes defined above, and could not be located under
  ;; resources/public, send the not found response.
  (route/not-found "Page not found"))


;;
;; Force requests to / to go to the index.html file in
;; resources/public. This function returns a function
;; that takes one argument: the ring request hash, which
;; contains other hashes.
;;
;; This function calls update-in to change the uri of
;; the request: if the uri matches /, it gets changed
;; to /index.html.
;;
(defn wrap-dir-index
  "Middleware to force request for / to return index.html"
  [handler]
  (fn [req]
    (handler
     (update-in req [:uri]
                #(if (= "/" %) "/index.html" %)))))

;;
;; Here we define our compojure application.
;; The threading operator used here (->) is confusing
;; at first. It has nothing to do operating system threads
;; or concurrency. It evaluates the first item, in this
;; case (handler/site main-routes), turns the result into
;; a list, then passes that list as the second item in
;; the next form. It evaluates the next form, turns the
;; result into a list, and passes that result as the second
;; item in the next form. The operator "threads" or weaves
;; the result of one evaluation into the next evaluation.
;;
;; The first line defines the routes for the site.
;; The next line adds a bit of middleware that enables
;; sessions using cookies. The following line (wrap-params),
;; adds another piece of middleware that extracts both
;; query string params (from a GET) and body params (from
;; POST and PUT) into a hash called "params" inside of
;; ring's standard request object.
;;
;; wrap-content-type adds middleware to intelligently
;; add content-type headers to responses. Finally,
;; wrap-dir-index calls our function above to re-route
;; / requests to /index.html.
;;
;; Note that all of these functions actually wrap around
;; the normal request-handling process, so the code below
;; causes the following chain of processing:
;;
;; 1. request comes in
;; 2. wrap-dir-index (the outer-most wrapper) alters the
;;    request and passes it to the next wrapper.
;; 3. wrap-content-type passes the request along without
;;    modifying it.
;; 4. wrap-params extracts all query string and form params,
;;    puts them into a single params hash, and sticks that
;;    hash back into the request before passing the request
;;    along.
;; 5. wrap-session extracts session data from the HTTP
;;    cookies, puts them a cookies hash, and puts that
;;    hash into the request before passing it along.
;; 6. Compojure checks the routing table to find the function
;;    that should handle this request. It calls that function,
;;    passing it a single param, which is the request object.
;; 7. The handler function returns the response, and
;;    wrap-session, being the inner-most wrapper, gets its
;;    hands on it. wrap-session converts the :session key
;;    from the response into a cookie that can be sent back
;;    to the browser. It passes the response on.
;; 8. wrap-params gets the request, and passes it along
;;    to the next handler, since it has no work to do at
;;    this point.
;; 9. wrap-content-type adds the appropriate content-type
;;    HTTP header to the response.
;; 10. wrap-dir-index returns the result of the handler to
;;     the client without altering it.
;;
;; The threading operator (->) effectively produces a
;; function that is like an onion. The first form after
;; the -> becomes the core of the onion. Subsequent
;; forms become the outer layers. Any data coming into
;; that first form must pass through all the other functions
;; on the way into the core, and then again on the way out.
;; Each function can alter the data both on its way into
;; the core and on it's way out. The outermost function is
;; the one at the bottom. It's the first to touch the data
;; on its way in, and the last to touch it on its way out.
;;
(def app (-> (handler/site main-routes)
             (wrap-session {:store (cookie-store)})
             wrap-params
             wrap-content-type
             wrap-dir-index))


;;
;; When we told leiningen above to compile this file into
;; a Java class, we told it this class would contain the
;; main method.
;;
;; When you specify gen-class, leiningen looks for methods
;; beginning with a dash, and turns those into public methods
;; that can be called from other Java classes.
;;
;; Clojure also provides a macro called defrecord, which
;; allows you to write pure Java classes in Clojure. You can
;; pre-compile those classes to Java bytecode and use them
;; as native Java objects from any JVM language. In fact,
;; writing Java classes with defrecord is quite a bit easier
;; than writing Java classes in Java.
;;
;; In the main method below, if we happen to be running as
;; a stand-alone Java application (instead of running through
;; leiningen), the var *command-line-args* will contain a
;; list of command-line arguments.
;;
;; Here, we tell our app to start the embedded Jetty web
;; server on whatever port was specified in the first
;; command-line argument, or on port 8080 if there were
;; no args on the command line.
;;
;; When you run this app using "lein ring server-headless",
;; it will run by default on port 3000, because that's
;; just what ring does!
;;
;;
(defn -main
  "This is the main entry point for the application."
  [& args]
  (println *command-line-args*)
  (let [port (or (first *command-line-args*) 8080)]
    (jetty/run-jetty app {:port port})))

