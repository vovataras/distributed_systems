# Lab_7: Seminar06. Message Queues

## Pre-requirements:
* Java 8 or higher;
* Maven;

## What to do:
1. Open server folder in console or terminal (the server was provided by the teacher);
2. Run server as following:

       mvn clean compile exec:java -D exec.args="0.0.0.0 61616"

   * arguments(args) can be yours (you need to enter the `host` and `port`), and you can omit them (the server will run with `"localhost 616161"` settings by default).
   
3. Open client folder in console or terminal;
4. Run client as following:

       mvn clean compile exec:java -D exec.args="localhost 61616"

   * arguments may be yours, but you need to enter the `host` and `port` of the server according to which the server is running; 
   * you can omit them (the client will run with these settings by default).

## Commands format:

#### `ping`
Testing connection (no parameters).

#### `echo`
Testing sending message. Enter the command and message for echo:

    echo message for testing

#### `login`
Log in on server or create new user. Has 2 parameters (login, password):

    login user_login user_password

#### `list`
Displays active users (no parameters).

#### `msg`
Send message to user. Has 2 parameters (recipient login, message(string)):

    msg to_user text message for another user

#### `file`
Send file to user. Has 2 parameters (recipient login, path to file):

    file to_user /path/to/file

#### `exit`
Shutdown the client (no parameters);

### Bonus task:

Application automatically answers that user is away to an incoming message when the user is not specifying any commands longer than for 5 minutes.