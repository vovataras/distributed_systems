package lpi.client.rest;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class ConnectionHandler implements Closeable {

    private Client client;  // jersey REST client
    private BufferedReader reader;
    private String targetURL;

    private boolean exit = false;
    private boolean isLoggedIn = false;

//    // unique session ID (need to login)
//    private String sessionId;

    public ConnectionHandler(Client client, String targetURL) {
        this.client = client;
        this.targetURL = targetURL;

        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    public void close() throws IOException {
        client = null;

        if (reader != null)
            reader.close();
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
        try{
            switch (command[0]) {
                case "ping":
                    ping();
                    break;
                case "echo":
                    echo(command);
                    break;
                case "login":
                    System.out.println("Login\n");
//                    login(command);
                    break;
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
//        } catch (ArgumentFault | ServerFault | LoginFault fault) {
//            System.out.println(fault.getMessage() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
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


    private void ping() {
        String response = client.target(targetURL + "/ping")
                .request(MediaType.TEXT_PLAIN_TYPE).get(String.class);

        System.out.println(response + "\n");
    }


    private void echo(String[] command) {
        String[] echoMessage = Arrays.copyOfRange(command, 1, command.length);

        String response = client.target(targetURL+ "/echo")
                .request(MediaType.TEXT_PLAIN_TYPE)
                .post(Entity.text(String.join("", echoMessage)), String.class);

        System.out.println(response + "\n");
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
