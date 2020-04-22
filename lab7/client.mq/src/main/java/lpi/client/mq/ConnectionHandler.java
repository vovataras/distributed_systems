package lpi.client.mq;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.Message;
import javax.jms.TextMessage;


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
                getResponse(userCommand);
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


    private void getResponse(String[] command) {
        try {
            switch ( command[0] ) {
                case "ping":
                    ping();
                    break;
                case "echo":
                    echo(command);
                    break;
//                case "login":
//                    login(command);
//                    break;
//                case "list":
//                    if (loggedIn())
//                        list();
//                    break;
//                case "msg":
//                    if (loggedIn())
//                        msg(command);
//                    break;
//                case "file":
//                    if (loggedIn())
//                        file(command);
//                    break;
                case "exit":
                    this.exit = true;
                    close();
                    return;
                case "help":
                    help();
                    break;

                default:
                    System.out.println("Not found this command...\n");
                    break;
            }
        } catch (Exception e) {
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



    private void ping() throws JMSException {
        Message msg = session.createMessage(); // an empty message.

        if (sendMessage(msg,"chat.diag.ping", "ping"))
            System.out.println("Ping success!\n");
//        else {
//            // TODO:
//            System.out.println("Ping error!\n");
//        }
    }



    private void echo(String[] command) throws JMSException {
        String[] echoMessage = Arrays.copyOfRange(command, 1, command.length);

        TextMessage msg =
                session.createTextMessage
                        (String.join(" ", echoMessage)); // a message that contains a string.

        if (!(sendMessage(msg, "chat.diag.echo", "echo")))
            System.out.println("Unexpected error!\n");
    }



    private boolean sendMessage(Message msg, String queueName, String command) throws JMSException {
        boolean isSuccess = false;

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


        switch (command) {
            case "ping":
            case "exit":
                if(!(replyMsg instanceof TextMessage)) {
                    isSuccess = true;
                }
                break;
            case "echo":
                if(replyMsg instanceof TextMessage) {
                    isSuccess = true;

                    // expect the text message as the content
                    String content = ((TextMessage) msg).getText(); // obtaining content.
                    System.out.println(content + "\n");
                }
//                else { // oops, the message is not TextMessage.
//                    throw new IOException("Unexpected message type: " + msg.getClass());
//                }
                break;


//            case "login":
//                if(!(msg instanceof TextMessage)) {
//                    isSuccess = true;
//
////                    String content = null; // we expect the text message as the content.
////                    if(msg instanceof TextMessage) { // therefore, we check for TextMessage.
////                        content = ((TextMessage)msg).getText(); // obtaining content.
////                    } else { // oops, the message is not TextMessage.
////                        throw new IOException(“Unexpected message type: ” + msg.getClass());
////                    }
//                } else {
//                    // we expect the text message as the content.
//                    String content = ((TextMessage)msg).getText(); // obtaining content.
//
//                    System.out.println(content + "\n");
//                }
//                break;
        }


        // if unexpected input message
        if (!isSuccess) {
            // expect the text message as the error
            String content = ((TextMessage)msg).getText(); // obtaining content
            System.out.println(content + "\n");
        }


        // close producer and consumer:
        consumer.close();
        producer.close();

        return isSuccess;
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
        System.out.println("exit  - close the client.\n");
    }
}