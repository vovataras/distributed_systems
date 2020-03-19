package lpi.server.rmi;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.Arrays;

public class ConnectionHandler implements Closeable {

    private IServer proxy;
    private BufferedReader reader;

    private boolean exit = false;

    // unique session ID (need to login)
    private String sessionId;

    public ConnectionHandler(IServer proxy) {
        this.proxy = proxy;

        reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Hello, what do you want to do?");
        System.out.println("Use the \"help\" command to get help.\n");
    }



    public void run() {
        try{
            while(!exit) {
                String[] userCommand = getUserCommand();
                if (userCommand[0].equalsIgnoreCase("exit"))
                    exit = true;

                getResponse(userCommand);
            }

            close();
        } catch (IOException e) {
            e.getStackTrace();
        }
    }



    String[] getUserCommand() throws IOException {
        String userCommand;
        String[] command;

        while(true) {
            userCommand = reader.readLine();

            if (userCommand.length() != 0){
                command = userCommand.split(" ");
                break;
            } else {
                System.out.println("Please enter command!\n");
            }
        }

        return command;
    }





    void getResponse(String[] command) {
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
                    this.sessionId = proxy.login(command[1], command[2]);
                    System.out.println("Login ok.\n");
                    break;
                case "list":
                    // TODO: write "list" method
                    String[] response = proxy.listUsers(this.sessionId);
                    break;
                case "msg":
                    // TODO: write "msg" method
                    String[] message = Arrays.copyOfRange(command, 2, command.length);
                    proxy.sendMessage(this.sessionId,
                                      new IServer.Message(command[1], String.join("", message)));
                    System.out.println("Message successfully sent.\n");
                    break;
                case "file":
                    // TODO: write "file" method
                    //proxy.sendFile(this.sessionId,
                    //               new IServer.FileInfo());
                    System.out.println("Send File");
                    break;
                case "receive_msg":
                    // TODO: write "receiveMsg" method
                    IServer.Message receivedMessage =  proxy.receiveMessage(sessionId);
                    break;
                case "receive_file":
                    // TODO: write "receiveFile" method
                    IServer.FileInfo receivedFile =  proxy.receiveFile(sessionId);
                    break;
                case "exit":
                    if (sessionId != null)
                        proxy.exit(sessionId);

                    return;
                case "help":
                    help();
                    break;

                default:
                    System.out.println("Not found this command...\n");
                    break;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void help(){
        System.out.println("  ping  - test the ability of the source computer to reach a server;");
        System.out.println("  echo  - display line of text/string that are passed as an argument;");
        System.out.println("  login - establish a new session with the server;");
        System.out.println("  list  - list all users on the server;");
        System.out.println("  msg   - send a message to a specific user;");
        System.out.println("  file  - send a file to a specific user;");
        System.out.println("  exit  - close the client.\n");
    }

    @Override
    public void close() throws IOException {

    }
}
