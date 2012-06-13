# ring-sample

This is a tiny Clojure application meant to demonstrate some of the features
of Clojure and Ring. 

The code is heavily commented, so take some time to poke around.

This app does not do any proper validation of form data, so please don't
look at it as a model for safe form handling!

## Usage

You will need leiningen to run this project. You can start the application
on port 3000 by running:

    lein ring server-headless

You should be able to reach the running application from your browser at
http://localhost:3000.

You can build the program into a standalone executable using:

    lein uberjar 

Once the jar is built, you can run it with:

    java -jar ring-sample ring-sample-1.0.0-SNAPSHOT.jar <port>

where port is the optional port number on which to listen for http connections.

Note that the lein commands must be run in the top-level directory. That's the
directory that contains this README and the project.clj file.

## License

Copyright (C) 2012 Andrew Diamond

Feel free to use this as you please!


