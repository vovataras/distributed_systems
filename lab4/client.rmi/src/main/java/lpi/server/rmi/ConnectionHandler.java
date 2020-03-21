package lpi.server.rmi;

import java.io.*;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Timer;

public class ConnectionHandler implements Closeable {

    private IServer proxy;
    private BufferedReader reader;

    private boolean exit = false;

    // unique session ID (need to login)
    private String sessionId;

    private Timer timer;


    public ConnectionHandler(IServer proxy) {
        this.proxy = proxy;
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }


    @Override
    public void close() throws IOException {
        if (proxy != null){
            if (sessionId != null) {
                proxy.exit(sessionId);
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
                    proxy.ping();
                    System.out.println("ping success!\n");
                    break;
                case "echo":
                    String[] echoMessage = Arrays.copyOfRange(command, 1, command.length);
                    System.out.println(proxy.echo(String.join(" ", echoMessage)) + "\n");
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
        }
        catch (RemoteException e) {
            System.out.println(e.getMessage() + "\n");
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


    private void login(String[] command) throws RemoteException {
        if (command.length < 3) {
            System.out.println("You need to enter your login and password!\n");
            return;
        } else if (command.length > 3) {
            System.out.println("Wrong params!\n");
            return;
        }

        this.sessionId = proxy.login(command[1], command[2]);
        System.out.println("Login ok.\n");

        Monitoring monitoring = new Monitoring(this.proxy, this.sessionId);
        // running timer task as daemon thread
        timer = new Timer(true);
        // start checking messages, files, and users with a certain frequency
        timer.scheduleAtFixedRate(monitoring, 0, 5*1000);
    }


    private void list() throws RemoteException {
        String[] users = proxy.listUsers(this.sessionId);

        System.out.println("Number of users on the server: " + users.length + ".");
        if (users.length > 0){
            System.out.println("Users:");
            for (String user: users) {
                System.out.println("  "+ user);
            }
            System.out.println();
        }
    }


    private void msg(String[] command) throws RemoteException {
        if (command.length < 2) {
            System.out.println("You need to enter receiver login!");
            return;
        }
        if (command.length < 3) {
            System.out.println("You need to enter a message!");
            return;
        }

        String[] message = Arrays.copyOfRange(command, 2, command.length);
        proxy.sendMessage(this.sessionId,
                new IServer.Message(command[1], String.join(" ", message)));
        System.out.println("Message successfully sent.\n");
    }


    private void file(String[] command) throws RemoteException {
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

        IServer.FileInfo fileInfo = null;
        try {
            fileInfo = new IServer.FileInfo(command[1], file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        proxy.sendFile(this.sessionId, fileInfo);
        System.out.println("File successfully sent.\n");
    }


    // TODO: write some tips
    private void help(){
        System.out.println(" ping  - test the ability of the source computer to reach a server;");
        System.out.println(" echo  - display line of text/string that are passed as an argument;");
        System.out.println(" login - establish a new session with the server;");
        System.out.println(" list  - list all users on the server;");
        System.out.println(" msg   - send a message to a specific user;");
        System.out.println(" file  - send a file to a specific user;");
        System.out.println(" exit  - close the client.\n");
    }
}