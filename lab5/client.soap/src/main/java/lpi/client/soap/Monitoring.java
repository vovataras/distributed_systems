package lpi.client.soap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

class Monitoring extends TimerTask {

    private ArrayList<String> currUsers = new ArrayList<>();
    private ArrayList<String> oldUsers = new ArrayList<>();

    private IChatServer serverProxy;
    private String sessionId;

    Monitoring(IChatServer serverProxy, String sessionId) {
        this.serverProxy = serverProxy;
        this.sessionId = sessionId;
    }


    @Override
    public void run() {
        try {
            if(loggedIn())
                checkUsers();

            if(loggedIn())
                receiveMsg();

            if(loggedIn())
                receiveFile();

        } catch (ArgumentFault | ServerFault fault) {
            System.out.println(fault.getMessage() + "\n");
        }
    }


    private boolean loggedIn(){
        if (this.sessionId != null){
            return true;
        } else {
            System.out.println("You need to login!\n");
            return false;
        }
    }


    private void receiveMsg() throws ArgumentFault, ServerFault {
        Message receivedMessage =  serverProxy.receiveMessage(sessionId);
        if (receivedMessage != null) {
            System.out.println("You have a new message!");
            System.out.println("From: " + receivedMessage.getSender());
            System.out.println("Message: " + receivedMessage.getMessage() + "\n");
        }
    }


    private void receiveFile() throws ArgumentFault, ServerFault {
        String folderPath = "./receivedFiles";

        FileInfo receivedFile =  serverProxy.receiveFile(sessionId);

        if (receivedFile != null) {
            System.out.println("You have a new file!");
            System.out.println("From: " + receivedFile.getSender());
            System.out.println("File: " + receivedFile.getFilename() + "\n");

            // check if there is a folder to save the files
            File folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdir();
            }

            // create a file and write bytes to it
            try (FileOutputStream stream =
                         new FileOutputStream(folder.getPath() + "/" + receivedFile.getFilename())) {
                stream.write(receivedFile.fileContent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void checkUsers() throws ArgumentFault, ServerFault {
        List<String> usersOnServer = serverProxy.listUsers(this.sessionId);

        if (usersOnServer.size() != 0){
            if (oldUsers.size() == 0) {
                for (String user: usersOnServer) {
                    oldUsers.add(user);
                    System.out.println(user + " is logged in.");
                }
                System.out.println();
            } else {
                // Add all users to currentUsers
                currUsers.addAll(usersOnServer);

                // compare with oldUsers and print new users on server
                for (String user: currUsers) {
                    if (!oldUsers.contains(user)) {
                        System.out.println(user + " is logged in.");

                        // Bonus task
                        // form the Message to send
                        Message message = new Message();
                        message.setReceiver(user);
                        message.setMessage("Hello there, " + user + "!");
                        // and send it
                        serverProxy.sendMessage(this.sessionId, message);
                    }
                }

                // compare with oldUsers and then print users which logged out from server
                for (String user: oldUsers) {
                    if (!currUsers.contains(user)) {
                        System.out.println(user + " is logged out.");
                    }
                }

                // move current users to old, and remove current
                oldUsers = (ArrayList<String>) currUsers.clone();
                currUsers.clear();
            }
        }
    }
}