package lpi.server.rmi;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TimerTask;

class Monitoring extends TimerTask {

    private ArrayList<String> currUsers = new ArrayList<>();
    private ArrayList<String> oldUsers = new ArrayList<>();

    private IServer proxy;
    private String sessionId;

    Monitoring(IServer proxy, String sessionId) {
        this.proxy = proxy;
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

            Thread.sleep(1000);
        } catch (RemoteException | InterruptedException e) {
            e.printStackTrace();
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


    // TODO: check "receiveMsg" method
    private void receiveMsg() throws RemoteException {
        IServer.Message receivedMessage =  proxy.receiveMessage(sessionId);
        if (receivedMessage != null) {
            System.out.println("You have a new message!");
            System.out.println("From: " + receivedMessage.getSender());
            System.out.println("Message: " + receivedMessage.getMessage() + "\n");
        }
    }


    // TODO: check "receiveFile" method
    private void receiveFile() throws RemoteException {
        String folderPath = "./receivedFiles";

        IServer.FileInfo receivedFile =  proxy.receiveFile(sessionId);

        if (receivedFile != null) {
            System.out.println("You have a new file!");
            System.out.println("From: " + receivedFile.getSender());
            System.out.println("File: " + receivedFile.getFilename() + "\n");

            File folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdir();
            }

            try {
                receivedFile.saveFileTo(folder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void checkUsers() throws RemoteException {
        // TODO: write comments
        String[] usersOnServer = proxy.listUsers(this.sessionId);

        if (usersOnServer.length != 0){
            if (oldUsers.size() == 0) {
                for (String user: usersOnServer) {
                    oldUsers.add(user);
                    System.out.println(user + " is logged in.");
                }
                System.out.println();
            } else {
                // Add all users to currentUsers
                currUsers.addAll(Arrays.asList(usersOnServer));

                for (String user: currUsers) {
                    if (!oldUsers.contains(user)) {
                        System.out.println(user + " is logged in.");
                    }
                }

                for (String user: oldUsers) {
                    if (!currUsers.contains(user)) {
                        System.out.println(user + " is logged out.");
                    }
                }

                oldUsers = (ArrayList<String>) currUsers.clone();
                currUsers.clear();
            }
        }
    }
}