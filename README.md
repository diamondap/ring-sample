# ring-sample

This is a tiny Clojure application meant to demonstrate some of the features
of Clojure and Ring. 

The application displays records from a SQLite database and lets you add new
records. It also displays records from a tab-delimited text file, and lets
you add new records to that (though those records will not be persisted).

The code is heavily commented, so take some time to poke around.

Among the things you'll see demonstrated in the code:

* Hot to set up a Leiningen project (project.clj)
* How to use, require and include namespaces and classes (most clj files)
* How to use Java interop (util.clj)
* How to use map with anonymous functions (util.clj)
* How to define and use regular expressions (util.clj)
* How to read data from a file (flat-file.clj)
* How to use refs to safely update shared data (flat-file.clj)
* How to set up RESTful routes with Compojure (core.clj)
* What a Ring request hash looks like (util.clj)
* How to access a database using Clojure JDBC (sqlite.clj)
* How to turn a Clojure namespace into a Java class (core.clj)

This app does not do any proper validation of form data, so please don't
look at it as a model for safe form handling!

## Usage

You will need leiningen to run this project. You can that here:

https://github.com/technomancy/leiningen/downloads

Once leiningen is installed, it will take care of the rest of the
dependencies for you.

You can start the application on port 3000 by running:

    lein ring server-headless

You should then be able to reach the running application from your 
browser at http://localhost:3000.

You can build the program into a standalone executable using:

    lein uberjar 

Once the jar is built, you can run it with:

    java -jar ring-sample ring-sample-1.0.0-SNAPSHOT.jar <port>

where port is the optional port number on which to listen for http connections.
If you don't specify a port, the application will run on port 8080.

Note that the lein commands must be run in the top-level directory. That's the
directory that contains this README and the project.clj file.

## License

Copyright (C) 2012 Andrew Diamond

Feel free to use this as you please!

