# Lab_3: TCP Sockets

## Pre-requirements:
* Java 8 or higher;
* Maven;

## What to do:
1. Open server folder in console or terminal (the server was provided by the teacher);
2. Run server as following:

		mvn clean compile exec:java -D exec.args="0.0.0.0 4321"

   * arguments can be yours, and you can omit them (the server will run with these settings is the default).
   
3. Open client folder in console or terminal;
4. Run client as following:

		mvn clean compile exec:java -D exec.args="0.0.0.0 4321"

   * args may be yours, but according to which the server is running, and you can omit them (the server will run with `args="0.0.0.0 4321"`).
