package client.tcp;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;


public class ConnectionHandler implements Closeable{
    private Socket socket;
    private CommandManager commandManager;
    private volatile boolean isClosing = false;
    private UserInput userInput;
    private String[] users;

    private byte currentCommand;

    DataInputStream readStream;
    DataOutputStream outputStream;

    public ConnectionHandler(Socket socket, CommandManager commandManager) {
        this.socket = socket;
        this.commandManager = commandManager;
        this.userInput = new UserInput();

        try {
            readStream = new DataInputStream(this.socket.getInputStream());
            outputStream = new DataOutputStream(this.socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        close(false);
    }

    private void close(boolean selfClose) {
        this.isClosing = true;

        if (this.socket != null) {
            try {
                this.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.socket = null;
        }
    }

    public void run() {
        try {
//            DataInputStream readStream = new DataInputStream(this.socket.getInputStream());
//            DataOutputStream outputStream = new DataOutputStream(this.socket.getOutputStream());
            MyThread myThread = null;

            while (!this.isClosing) {
                byte[] request = null;

                String[] userCommand = userInput.getUserCommand();

                if (userCommand.length == 0) {
                    continue;
                }

                if (userCommand[0].getBytes()[0] == (byte)127) {
                    close();
                    if (myThread != null)
                        myThread.interrupt();
                    break;
                }

                currentCommand = userCommand[0].getBytes()[0];


                request = commandManager.execute(userCommand);
                if (request.length == 0)
                    continue;

                commandManager.sendRequest(request, this.outputStream);



//                if (request == null) {
//                    System.out.println("ERROR: Unable to send request to the server.");
//                }
//
//                // Sending Request to the server.
//                outputStream.writeInt(request.length);
//                outputStream.write(request);

                // wait for the server to read the socket messages and reply
                for (int i = 0; i< 3; i++)
                    System.out.print(".");
                Thread.sleep(333);
                System.out.println();
//                Thread.sleep(1000);

                // TODO: rewrite comments
//                // read the size of reply and response message itself
//                int contentSize = readStream.readInt();
////                System.out.println(contentSize);
//                byte[] response = new byte[contentSize];
//                readStream.readFully(response);
////                System.out.println(response);
                byte[] response = commandManager.receiveResponse(this.readStream);

                // TODO: explain
                if (response[0] == (byte)6 || response[0] == (byte)7) {
                    myThread = new MyThread(this.commandManager, this.socket);
                    myThread.start();
                }
                // and decode it
                this.commandManager.decode(this.currentCommand, response);
            }
        }
        catch (IOException | InterruptedException ex) {
            ex.getStackTrace();
        }
    }
}



class MyThread extends Thread {

    private CommandManager commandManager;

    private byte[] recieveMsgRequest;
    private byte[] recieveFileRequest;
    private byte[] recieveListRequest;
    Socket socket;

    DataInputStream readStream;
    DataOutputStream outputStream;
    ArrayList<String> currUsers = new ArrayList<String>();
    ArrayList<String> oldUsers = new ArrayList<String>();

    private boolean isEncoded = false;

    MyThread(CommandManager commandManager, Socket socket) {
        this.commandManager = commandManager;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            this.readStream = new DataInputStream(socket.getInputStream());
            this.outputStream = new DataOutputStream(socket.getOutputStream());

            if (!isEncoded) {
                recieveMsgRequest = new byte[] {25};
                recieveFileRequest = new byte[] {30};
                recieveListRequest = new byte[] {10};

                isEncoded = true;
            }

            while (!socket.isClosed()){
                try {
                    checkUsers();
                    runCommand(recieveMsgRequest);
                    runCommand(recieveFileRequest);

                }catch (InterruptedException e) {
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void runCommand(byte[] cmd) throws InterruptedException, IOException {
        Thread.sleep(2000);
        commandManager.sendRequest(cmd, this.outputStream);
        Thread.sleep(500);

        // TODO: write comments
        byte[] response = commandManager.receiveResponse(this.readStream);
        // and decode it
        this.commandManager.decode(cmd[0], response);
    }


    private void checkUsers() throws IOException, InterruptedException {
        Thread.sleep(1000);
        commandManager.sendRequest(recieveListRequest, this.outputStream);
        Thread.sleep(500);

        // TODO: write comments
        byte[] response = commandManager.receiveResponse(this.readStream);
        // and decode it
        String[] usersOnServer = commandManager.getActiveUsers(response);

        if (usersOnServer.length != 0){
            if (oldUsers.size() == 0) {
                for (String user: usersOnServer) {
                    oldUsers.add(user);
                    System.out.println(user + " is logged in.");
                }
            } else {
                for (String user: usersOnServer) {
                    currUsers.add(user);
                }

                for (String user: currUsers) {
                    if (!oldUsers.contains(user))
                        System.out.println(user + " is logged in.");
                }

                for (String user: oldUsers) {
                    if (!currUsers.contains(user))
                        System.out.println(user + " is logged out.");
                }

                oldUsers = (ArrayList<String>) currUsers.clone();
                currUsers.clear();
            }

        }
    }

}
