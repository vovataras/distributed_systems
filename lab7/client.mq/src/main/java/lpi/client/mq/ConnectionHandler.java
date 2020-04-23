package lpi.client.mq;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.MapMessage;
import javax.jms.ObjectMessage;
import lpi.server.mq.FileInfo;


public class ConnectionHandler implements Closeable {

    private Session session;
    private BufferedReader reader;

    private boolean exit = false;
    private boolean isLoggedIn = false; // to see if the user is logged in


    public ConnectionHandler(Session session) {
        this.session = session;

        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }


    public void close() throws IOException {
        try {
            exit();
        } catch (JMSException e) {
            e.printStackTrace();
        }

        session = null;

        if (reader != null) {
            reader.close();
        }
    }


    public void run() {
        System.out.println("Hello, what do you want to do?");
        System.out.println("Use the \"help\" command to get help.\n");

        try{
            while(!exit) {
                String[] userCommand = getUserCommand();
                callCommand(userCommand);
            }
        } catch (IOException e) {
            e.getStackTrace();
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
            e.printStackTrace();
            System.out.println();
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



    private void ping() throws JMSException {
        Message msg = session.createMessage(); // an empty message

        Message response = getResponse(msg, QueueName.ping);

        if (!(response instanceof TextMessage)){
            System.out.println("Ping success!\n");
        } else {
            // TODO: create method for errors (for TextMessage)
            System.out.println("Ping error!\n");
        }
    }



    private void echo(String[] command) throws JMSException, IOException {
        String[] echoMessage = Arrays.copyOfRange(command, 1, command.length);

        TextMessage msg =
                session.createTextMessage
                        (String.join(" ", echoMessage)); // a message that contains a string

        Message response = getResponse(msg, QueueName.echo);

        if (response instanceof TextMessage) {
            // expect the text message as the content
            String content = ((TextMessage) response).getText(); // obtaining content.
            System.out.println(content + "\n");
        } else {
            // oops, the message is not TextMessage.
            System.out.println("Unexpected error!\n");
            throw new IOException("Unexpected message type: " + response.getClass());
        }
    }



    private void login(String[] command) throws JMSException {
        if (isLoggedIn){
            System.out.println("You must log out first!\n");
            return;
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

        Message response = getResponse(msg, QueueName.login);

        if (response instanceof MapMessage){
            if (((MapMessage) response).getBoolean("success")) {
                // when user logged in successfully
                isLoggedIn = true;
                // print success message
                System.out.println(((MapMessage) response).getString("message") + "\n");
            } else {
                // print error
                isLoggedIn = false;
                System.out.println("Failed to login: " + ((MapMessage) response).getString("message") + "\n");
            }
        } else {
            // TODO: create method for errors (for TextMessage)
            System.out.println("Unexpected error!\n");
        }
    }



    private void list() throws JMSException {

        Message msg = session.createMessage();
        Message response = getResponse(msg, QueueName.list);

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
            System.out.println(((MapMessage) response).getString("message"));
        } else {
            // TODO: create method for errors (for TextMessage)
            System.out.println("Unexpected error!\n");
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
        msg.setString("message", String.join("", messageContent));

        Message response = getResponse(msg, QueueName.sendMsg);

        if (response instanceof MapMessage){
            if (((MapMessage) response).getBoolean("success")) {
                // when the message is successfully sent
                // print success message
                System.out.println(((MapMessage) response).getString("message") + "\n");
            } else {
                // print error
                System.out.println(((MapMessage) response).getString("message"));
                System.out.println("Please retry!\n");
            }
        } else {
            // TODO: create method for errors (for TextMessage)
            System.out.println("Unexpected error!\n");
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

        Message response = getResponse(msg, QueueName.sendFile);

        if (response instanceof MapMessage){
            if (((MapMessage) response).getBoolean("success")) {
                // when the file is successfully sent
                // print success message
                System.out.println(((MapMessage) response).getString("message") + "\n");
            } else {
                // print error
                System.out.println(((MapMessage) response).getString("message"));
                System.out.println("Please retry!\n");
            }
        } else {
            // TODO: create method for errors (for TextMessage)
            System.out.println("Unexpected error!\n");
            checkUnexpectedError(response);
        }
    }



    private void exit() throws JMSException {
        this.exit = true;

        if (isLoggedIn) {
            Message msg = session.createMessage(); // an empty message
            Message response = getResponse(msg, QueueName.exit);

            if (!(response instanceof TextMessage)) {
                System.out.println("Successful logout.");
            } else {
                // TODO: create method for errors (for TextMessage)
                System.out.println("Some error");
            }
        }

        System.out.println("Bye!\n");
    }



    private void checkUnexpectedError(Message response) throws JMSException {
        if (response instanceof TextMessage) {
            // expect the text message as the content
            String content = ((TextMessage) response).getText(); // obtaining content.
            System.out.println(content + "\n");
        }
    }



    private Message getResponse(Message msg, String queueName) throws JMSException {

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
        Message replyMsg = consumer.receive(2000);


        // if unexpected input message
//        if (!isSuccess) {
//            // expect the text message as the error
//            String content = ((TextMessage)msg).getText(); // obtaining content
//            System.out.println(content + "\n");
//        }


        consumer.close();
        producer.close();

        return replyMsg;
    }





    private void help() {
        System.out.println("ping  - test the ability of the source computer to reach a server;\n");
        System.out.println("echo  - display line of text/string that are passed as an argument;\n" +
                " Format: echo <message>\n");
        System.out.println("login - establish a new session with the server;\n" +
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