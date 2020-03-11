package client.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class UserInput implements Const{
    private BufferedReader reader;

    UserInput() {
        reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Hello, what do you want to do?");
        System.out.println("Use the \"help\" command to get help.\n");
    }

    String[] getUserCommand() throws IOException {
        // TODO: return object instead of string
        String userCommand;
        String[] command =  new String[0];
        boolean isEmptyCommand = true;
        
        while(isEmptyCommand) {
            isEmptyCommand = false;
            userCommand = reader.readLine();
    
            command = userCommand.split(" ");
    
            switch (command[0]) {
                case "ping":
                    command[0] = new String(new byte[] {CMD_PING});
                    break;
                case "echo":
                    command[0] = new String(new byte[] {CMD_ECHO});
                    break;
                case "login":
                    command[0] = new String(new byte[] {CMD_LOGIN});
                    break;
                case "list":
                    command[0] = new String(new byte[] {CMD_LIST});
                    break;
                case "msg":
                    command[0] = new String(new byte[] {CMD_MSG});
                    break;
                case "file":
                    command[0] = new String(new byte[] {CMD_FILE});
                    break;
                case "receive_msg":
                    command[0] = new String(new byte[] {CMD_RECEIVE_MSG});
                    break;
                case "receive_file":
                    command[0] = new String(new byte[] {CMD_RECEIVE_FILE});
                    break;
                case "exit":
                    command[0] = new String(new byte[] {CMD_EXIT});
                    break;
                case "help":
                    isEmptyCommand = true;
                    help();
                    break;

                default:
                    if (userCommand.length() == 0){
                        System.out.println("Please enter command!\n");
                    } else {
                        // if not found command, repeat again
                        System.out.println("Not found this command...\n");
                    }
                    isEmptyCommand = true;
                    break;
            }
        }

        return command;
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
}
