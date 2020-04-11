package lpi.client.rest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
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


    private void login(String[] command) {
        if (command.length < 3) {
            System.out.println("You need to enter your login and password!\n");
            return;
        } else if (command.length > 3) {
            System.out.println("Wrong params!\n");
            return;
        }

        UserInfo userInfo = new UserInfo();
        userInfo.login = command[1];
        userInfo.password = command[2];
        Entity userInfoEntity = Entity.entity(userInfo,
                MediaType.APPLICATION_JSON_TYPE);
        Response response = client.target(targetURL + "/user")
                .request().put(userInfoEntity);


        if (response.getStatus() == Status.CREATED.getStatusCode())
            System.out.println("New user registered\n");
        else if (response.getStatus() == Status.ACCEPTED.getStatusCode())
            System.out.println("Login ok.\n");
        else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
            System.out.println("Request body is not specified or invalid\n");
            return;
        } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
            System.out.println("Provided login/password pair is invalid\n");
            return;
        } else if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
            System.out.println("Internal server error\n");
            return;
        }

        // register authentication feature, that will authenticate all further requests
        this.client.register(HttpAuthenticationFeature
                .basic(userInfo.login, userInfo.password));
        isLoggedIn = true;
    }


    private void list() {
        Response response = client.target(targetURL + "/users")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();


        String jsonResponse = client.target(targetURL + "/users")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(String.class);

        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray users = (JSONArray) jsonObject.get("items");

            System.out.println("Number of users on the server: " + users.length() + ".");

            if (users.length() > 0){
                System.out.println("Users:");

                for (int i = 0; i < users.length(); i++) {
                    System.out.println(i+1 + ": " + users.get(i));
                }
                System.out.println();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
