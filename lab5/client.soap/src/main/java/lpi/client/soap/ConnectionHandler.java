package lpi.client.soap;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;

public class ConnectionHandler implements Closeable {

    private IChatServer serverProxy;
    private BufferedReader reader;

    private boolean exit = false;

    // unique session ID (need to login)
    private String sessionId;

    private Timer timer;


    public ConnectionHandler(IChatServer serverProxy) {
        this.serverProxy = serverProxy;
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    public void close() throws IOException {
        if (serverProxy != null){
            if (sessionId != null) {
                try {
                    serverProxy.exit(sessionId);
                } catch (ArgumentFault | ServerFault fault) {
                    fault.printStackTrace();
                }
                sessionId = null;
            }
        }

        if (reader != null)
            reader.close();

        if (timer != null)
            timer.cancel();
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
                    serverProxy.ping();
                    System.out.println("ping success!\n");
                    break;
                case "echo":
                    String[] echoMessage = Arrays.copyOfRange(command, 1, command.length);
                    System.out.println(serverProxy.echo(String.join(" ", echoMessage)) + "\n");
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
        } catch (ArgumentFault | ServerFault | LoginFault fault) {
            System.out.println(fault.getMessage() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // check if user is logged in to the server
    private boolean loggedIn(){
        if (this.sessionId != null){
            return true;
        } else {
            System.out.println("You need to login!\n");
            return false;
        }
    }


    private void login(String[] command) throws ArgumentFault, LoginFault, ServerFault {
        if (command.length < 3) {
            System.out.println("You need to enter your login and password!\n");
            return;
        } else if (command.length > 3) {
            System.out.println("Wrong params!\n");
            return;
        }

        this.sessionId = serverProxy.login(command[1], command[2]);
        System.out.println("Login ok.\n");

        // create a monitoring object to check active users and receive new messages and files
        Monitoring monitoring = new Monitoring(this.serverProxy, this.sessionId);
        // running timer task as daemon thread
        timer = new Timer(true);
        // start checking messages, files, and users with a certain frequency
        timer.scheduleAtFixedRate(monitoring, 0, 3*1000);
    }


    private void list() throws ArgumentFault, ServerFault {
        List<String> users = serverProxy.listUsers(this.sessionId);

        System.out.println("Number of users on the server: " + users.size() + ".");
        if (users.size() > 0){
            System.out.println("Users:");
            for (String user: users) {
                System.out.println("  "+ user);
            }
            System.out.println();
        }
    }


    private void msg(String[] command) throws ArgumentFault, ServerFault {
        if (command.length < 2) {
            System.out.println("You need to enter receiver login!\n");
            return;
        }
        if (command.length < 3) {
            System.out.println("You need to enter a message!\n");
            return;
        }

        String[] messageContent = Arrays.copyOfRange(command, 2, command.length);

        // form a Message to send
        Message message = new Message();
        message.setReceiver(command[1]);
        message.setMessage(String.join(" ", messageContent));

        serverProxy.sendMessage(this.sessionId, message);
        System.out.println("Message successfully sent.\n");
    }


    private void file(String[] command) throws ArgumentFault, ServerFault, IOException {
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

        // convert a file to an byte array
        byte[] fileContent = Files.readAllBytes(file.toPath());

        // form a FileInfo to send
        FileInfo fileInfo = new FileInfo();
        fileInfo.setReceiver(command[1]);
        fileInfo.setFilename(file.getName());
        fileInfo.setFileContent(fileContent);

        serverProxy.sendFile(this.sessionId, fileInfo);
        System.out.println("File successfully sent.\n");
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
