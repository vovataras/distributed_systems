package lpi.client.mq;

import java.io.*;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.MapMessage;
import javax.jms.ObjectMessage;
import javax.jms.Destination;
import javax.jms.MessageConsumer;
import lpi.server.mq.FileInfo;


public class ConnectionHandler implements Closeable {

    private Session session;
    private Session sessionListener;
    private BufferedReader reader;
    private MessageConsumer messageConsumer;
    private MessageConsumer fileConsumer;

    private boolean shutdown = false;
    private boolean isLoggedIn = false; // to see if the user is logged in

    private Instant lastActionTime;     // to track the last user action time (for AFK messages)
    private String username;            // the login of the user logged from this client

    public ConnectionHandler(Session session, Session sessionListener) {
        this.session = session;
        this.sessionListener = sessionListener;

        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }


    public void close() throws IOException {
        try {
            if (messageConsumer != null)
                messageConsumer.close();

            if (fileConsumer != null)
                fileConsumer.close();

            shutdown();
        } catch (JMSException e) {
//            e.printStackTrace();
            System.out.println(e.getMessage() + "\n");
        }

        session = null;
        sessionListener = null;

        if (reader != null) {
            reader.close();
        }
    }


    public void run() {
        System.out.println("Hello, what do you want to do?");
        System.out.println("Use the \"help\" command to get help.\n");

        try{
            while(!shutdown) {
                String[] userCommand = getUserCommand();
                callCommand(userCommand);
            }
        } catch (IOException e) {
//            e.getStackTrace();
            System.out.println(e.getMessage() + "\n");
        }
    }


    private String[] getUserCommand() throws IOException {
        String userCommand;
        String[] command;

        while(true) {
            userCommand = this.reader.readLine();

            if (userCommand.length() != 0){
                command = userCommand.split(" ");
                break;
            } else {
                System.out.println("Please enter command!\n");
            }
        }

        return command;
    }


    private void callCommand(String[] command) {
        try {
            lastActionTime = Instant.now();

            switch ( command[0] ) {
                case "ping":
                    ping();
                    break;
                case "echo":
                    echo(command);
                    break;
                case "login":
                    login(command);
                    break;
                case "list":
                    if (loggedIn())
                        list();
                    break;
                case "msg":
                    if (loggedIn())
                        msg(command);
                    break;
                case "file":
                    if (loggedIn())
                        file(command);
                    break;
                case "exit":
                    close();
                    return;
                case "help":
                    help();
                    break;

                default:
                    System.out.println("Not found this command...\n");
                    break;
            }
        } catch (JMSException | IOException e) {
//            e.printStackTrace();
            System.out.println(e.getMessage() + "\n");
        }
    }


    // check if user is logged in to the server
    private boolean loggedIn(){
        if (this.isLoggedIn){
            return true;
        } else {
            System.out.println("You need to login!\n");
            return false;
        }
    }



    private void checkAFK() {
        Thread threadPing = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(20 *  1000);

                    Instant instantNow = Instant.now();
                    Duration timeElapsed = Duration.between(lastActionTime, instantNow);

                    if (timeElapsed.toMinutes() >= 5) {
                        break;
                    }

                    Message msg = session.createMessage(); // an empty message
                    getResponse(msg, QueueName.PING);
                } catch (Exception e) {
//                    e.printStackTrace();
//                    System.out.println(e.getMessage() + "\n");
                }
            }

            System.out.println("AFK!!!\n");

            try {
                String[] users = new String[0];
                Message msgList  = session.createMessage();
                Message response = getResponse(msgList, QueueName.LIST);

                if (response instanceof ObjectMessage) {
                    // receive “response” and ensure it is indeed ObjectMessage
                    Serializable obj = ((ObjectMessage)response).getObject();
                    if (obj instanceof String[]) {
                        users = (String[])obj;
                    }
                }

                for (String user : users) {
                    if (user.equals(username))
                        continue;

                    while (true) {
                        String messageContent = "Sorry, I’m AFK, will answer ASAP";

                        MapMessage msg = session.createMapMessage();
                        msg.setString("receiver", user);
                        msg.setString("message", messageContent);

                        response = getResponse(msg, QueueName.SEND_MSG);
                        if (response instanceof MapMessage) {
                            // when the message is successfully sent
                            if (((MapMessage) response).getBoolean("success"))
                                break;
                        }
                    }
                }

                isLoggedIn = false;
            } catch (Exception e) {
//                e.printStackTrace();
//                System.out.println(e.getMessage() + "\n");
            }
        });

        threadPing.setDaemon(true);
        threadPing.start();
    }



    private void ping() throws JMSException {
        Message msg = session.createMessage(); // an empty message

        Message response = getResponse(msg, QueueName.PING);

        if (!(response instanceof TextMessage)){
            System.out.println("Ping success!\n");
        } else {
            checkUnexpectedError(response);
        }
    }



    private void echo(String[] command) throws JMSException {
        String[] echoMessage = Arrays.copyOfRange(command, 1, command.length);

        TextMessage msg =
                session.createTextMessage
                        (String.join(" ", echoMessage)); // a message that contains a string

        Message response = getResponse(msg, QueueName.ECHO);

        if (response instanceof TextMessage) {
            // expect the text message as the content
            String content = ((TextMessage) response).getText(); // obtaining content
            System.out.println(content + "\n");
        } else {
            // oops, the message is not TextMessage.
            System.out.println("Unexpected error!");
            System.out.println("Unexpected message type: " + response.getClass() + "\n");
        }
    }



    private void login(String[] command) throws JMSException {
        if (isLoggedIn){
            System.out.println("You will be logged out automatically.");
            exit();
            System.out.println();
        }

        if (command.length < 3) {
            System.out.println("You need to enter your login and password!\n");
            return;
        } else if (command.length > 3) {
            System.out.println("Wrong params!\n");
            return;
        }

        MapMessage msg = session.createMapMessage();

        String login = command[1];
        String password = command[2];

        msg.setString("login", login);
        msg.setString("password", password);

        Message response = getResponse(msg, QueueName.LOGIN);

        if (response instanceof MapMessage){
            if (((MapMessage) response).getBoolean("success")) {
                // when user logged in successfully
                isLoggedIn = true;
                username = login;
                createMessageReceiver();
                createFileReceiver();
                checkAFK();
                // print success message
                System.out.println(((MapMessage) response).getString("message") + "\n");
            } else {
                // print error
                isLoggedIn = false;
                System.out.println("Failed to login: " + ((MapMessage) response).getString("message") + "\n");
            }
        } else {
            checkUnexpectedError(response);
        }
    }



    private void list() throws JMSException {
        Message msg = session.createMessage();
        Message response = getResponse(msg, QueueName.LIST);

        if (response instanceof ObjectMessage) {
            // receive “response” and ensure it is indeed ObjectMessage
            Serializable obj = ((ObjectMessage)response).getObject();
            if (obj instanceof String[]) {
                String[] users = (String[])obj;

                System.out.println("Number of users on the server: " + users.length + ".");
                if (users.length > 0){
                    System.out.println("Users:");
                    for (int i = 0; i < users.length; i++) {
                        System.out.println(i+1 + ": " + users[i]);
                    }
                    System.out.println();
                }
            }
        } else if (response instanceof MapMessage) {
            // print processing error
            System.out.println(((MapMessage) response).getString("message") + "\n");
        } else {
            checkUnexpectedError(response);
        }
    }



    private void msg(String[] command) throws JMSException {
        if (command.length < 2) {
            System.out.println("You need to enter receiver login!\n");
            return;
        }
        if (command.length < 3) {
            System.out.println("You need to enter a message!\n");
            return;
        }

        String receiver = command[1];
        String[] messageContent = Arrays.copyOfRange(command, 2, command.length);

        MapMessage msg = session.createMapMessage();
        msg.setString("receiver", receiver);
        msg.setString("message", String.join(" ", messageContent));

        Message response = getResponse(msg, QueueName.SEND_MSG);

        if (response instanceof MapMessage){
            // print success message or error
            System.out.println(((MapMessage) response).getString("message"));
            if (!((MapMessage) response).getBoolean("success")) {
                // when error
                System.out.println("Please retry!");
            }
            System.out.println();
        } else {
            checkUnexpectedError(response);
        }
    }



    private void file(String[] command) throws IOException, JMSException {

        if (command.length < 2) {
            System.out.println("You need to enter receiver login!\n");
            return;
        }
        else if (command.length < 3) {
            System.out.println("You need to enter a path to the file!!\n");
            return;
        }

        File file = new File(command[2]);
        if (!file.isFile()) {
            System.out.println("Incorrect file path or it is not a file.\n");
            return;
        }

        ObjectMessage msg = session.createObjectMessage();

        // convert a file to an byte array
        byte[] fileContent = Files.readAllBytes(file.toPath());

        // form a FileInfo to send
        FileInfo fileInfo = new FileInfo();
        fileInfo.setReceiver(command[1]);
        fileInfo.setFilename(file.getName());
        fileInfo.setFileContent(fileContent);

        msg.setObject(fileInfo);

        Message response = getResponse(msg, QueueName.SEND_FILE);

        if (response instanceof MapMessage){
            // print success message or error
            System.out.println(((MapMessage) response).getString("message"));
            if (!((MapMessage) response).getBoolean("success")) {
                // when error
                System.out.println("Please retry!");
            }
            System.out.println();
        } else {
            checkUnexpectedError(response);
        }
    }


    private void createMessageReceiver() throws JMSException {
        Destination queue = sessionListener.createQueue(QueueName.GET_MSG);
        messageConsumer = sessionListener.createConsumer(queue);
        messageConsumer.setMessageListener(new MessageReceiver());
    }



    private void createFileReceiver() throws JMSException {
        Destination queue = sessionListener.createQueue(QueueName.GET_FILE);
        fileConsumer = sessionListener.createConsumer(queue);
        fileConsumer.setMessageListener(new FileReceiver());
    }



    private void shutdown() throws JMSException {
        this.shutdown = true;
        exit();
        System.out.println("Bye!\n");
    }


    private void exit() throws JMSException {
        if (isLoggedIn) {
            Message msg = session.createMessage(); // an empty message
            Message response = getResponse(msg, QueueName.EXIT);

            if (!(response instanceof TextMessage)) {
                System.out.println("Successful logout.");
            } else {
                checkUnexpectedError(response);
            }
        }
    }



    // on unexpected input message
    private void checkUnexpectedError(Message response) throws JMSException {
        System.out.println("Unexpected error!");
        if (response instanceof TextMessage) {
            // expect the text message as the content
            String content = ((TextMessage) response).getText(); // obtaining content
            System.out.println(content);
        }
        System.out.println();
    }



     private synchronized Message getResponse(Message msg, String queueName) throws JMSException {
        // create an object specifying the Destination to which the message will be sent:
        javax.jms.Destination targetQueue = session.createQueue(queueName);

        // create an object specifying the Destination where the response will be received:
        javax.jms.Destination replyQueue = this.session.createTemporaryQueue();

        // create a producer on targetQueue, which will allow to send the request message:
        javax.jms.MessageProducer producer = session.createProducer(targetQueue);

        // create a consumer on replyQueue, which will allow to receive the answer:
        javax.jms.MessageConsumer consumer = session.createConsumer(replyQueue);

        // specify JMSReplyTo attribute:
        msg.setJMSReplyTo(replyQueue);

        // send the message using producer:
        producer.send(msg);

        // await the reply using consumer:
        Message replyMsg = consumer.receive(1500);

        consumer.close();
        producer.close();

        return replyMsg;
    }



    private void help() {
        System.out.println("ping  - test the ability of the source computer to reach a server;\n");
        System.out.println("echo  - display line of text/string that are passed as an argument;\n" +
                " Format: echo <message>\n");
        System.out.println("login - establish a new session with the server;\n" +
                " (if you are logged in, you will be logged out automatically)\n" +
                " Format: login <username> <password>\n");
        System.out.println("list  - list all users on the server;\n");
        System.out.println("msg   - send a message to a specific user;\n" +
                " User must be registered on the server!\n" +
                " Format: msg <receiver username> <message>\n");
        System.out.println("file  - send a file to a specific user;\n" +
                " User must be registered on the server!\n" +
                " Format: file <receiver username> </path/to/file>\n");
        System.out.println("exit  - log out and close the client.\n");
    }
}