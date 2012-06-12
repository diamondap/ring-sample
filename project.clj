(defproject ring-sample "1.0.0-SNAPSHOT"
  :description "A simple well-annotated ring application to demonstrate some of the features of Clojure."
  :dependencies [[org.clojure/clojure "1.4.0"]

                 ;; Ring provides support for handing web requests.
                 ;; It is similar to Ruby's Rack and Python's WSGI.
                 ;; It can run inside of Tomcat, or by itself.
                 ;; The Jetty adapter let us run as a stand-alone
                 ;; application with an embeded web server.
                 [ring/ring-core "1.1.0"]
                 [ring/ring-jetty-adapter "1.1.0"]

                 ;; Compojure lets us define RESTful routes.
                 [compojure "1.1.0"]

                 ;; Korma helps us generate SQL
                 [korma "0.3.0-beta9"]

                 ;; The following libraries are pure Java.
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [cheshire "4.0.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 [joda-time "2.0"]
                 [log4j "1.2.16" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]]

  ;; ring-devel will be loaded during development.
  ;; It allows us to edit code while the app is running,
  ;; and have the changes picked up immediately.
  :dev-dependencies [[ring/ring-devel "1.1.0"]]

  ;; These are leiningen plugins used during development.
  ;; lein-ring enables us to fire up our app as a web server.
  ;; It also can package the application as a war file or as
  ;; an uberwar, which will run as a stand-alone web server + app.
  ;; lein-swank lets us develop interactively using emacs.
  :plugins [[lein-ring "0.6.6"]
            [lein-swank "1.4.4"]]

  ;; This tells ring where to find the main entry-point
  ;; of the application.
  :ring {:handler ring-sample.core/app}

  ;; If we want to compile our project into a stand-alone application,
  ;; we can define a Java main method. This tells leiningen where
  ;; that method is.
  :main ring-sample.core

  ;; leiningen knows where to find useful repositories for
  ;; downloading jar files. We sometimes have to tell it about
  ;; additional repos to get obscure jars.
  ;; :repositories {
  ;; "xerial" "http://www.xerial.org/maven/repository/"
  ;; "some-other-repo" "http://repo.sample.kom/maven/"}

  ;; We can also tell leiningen what options to pass to the
  ;; JVM when it starts our application.
  :jvm-opts ["-server"
             "-Xms32M"
             "-Xmx256M"
             "-XX:NewRatio=5"
             "-XX:+UseConcMarkSweepGC"
             "-XX:+UseParNewGC"
             "-XX:MaxPermSize=64m"])
