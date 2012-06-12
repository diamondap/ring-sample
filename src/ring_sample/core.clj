(ns ring-sample.core
  (:use [compojure.core]
        [ring.util.response]
        [ring.middleware.session]
        [ring.middleware.session.cookie]
        [ring.middleware.content-type]
        [ring.middleware.params])
  (:require [clojure.tools.logging :as log]
            [ring.adapter.jetty :as jetty]
            [cheshire.core :as json]
            [compojure.route :as route]
            [compojure.handler :as handler]
            ;;[clarity.config :as config]
            ;;[clarity.data.blsoe :as blsoe]
            )
  (:gen-class :main true))


(defroutes main-routes
  ;;(GET "/sqlite" [] sqlite/index)
  ;;(GET "/flat-file" [] flat-file/index)
  (route/resources "/")
  (route/not-found "Page not found"))


(defn wrap-dir-index
  "Middleware to force request for / to return index.html"
  [handler]
  (fn [req]
    (handler
     (update-in req [:uri]
                #(if (= "/" %) "/index.html" %)))))

(def app (-> (handler/site main-routes)
             (wrap-session {:store (cookie-store)})
             wrap-params
             ;;wrap-gzip
             wrap-content-type
             wrap-dir-index))


(defn -main
  "This is the main entry point for the application."
  [& args]
  "Little ring/jetty server"
  (let [port (or (first *command-line-args*) 8080)]
            (jetty/run-jetty app {:port port})))

