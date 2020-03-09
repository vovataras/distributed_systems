package client.tcp;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


public class ConnectionHandler implements Closeable{
    private Socket socket;
    private CommandManager commandManager;
    private volatile boolean isClosing = false;
    private UserInput userInput;

    public ConnectionHandler(Socket socket, CommandManager commandManager) {
        this.socket = socket;
        this.commandManager = commandManager;
        this.userInput = new UserInput();
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
            DataInputStream readStream = new DataInputStream(this.socket.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(this.socket.getOutputStream());

            while (!this.isClosing) {
                byte[] request = null;

                String[] userCommand = userInput.getUserCommand();

                if (userCommand.length == 0) {
                    continue;
                }

                if (userCommand[0].getBytes()[0] == (byte)127) {
                    close();
                    break;
                }

                request = commandManager.execute(userCommand);

                if (request == null) {
                    System.out.println("ERROR: Unable to send request to the server.");
                }

                // Sending Request to the server.
                outputStream.writeInt(request.length);
                outputStream.write(request);

                // wait for the server to read the socket messages and reply
                for (int i = 0; i< 3; i++)
                    System.out.print(".");
                Thread.sleep(333);
                System.out.println();
//                Thread.sleep(1000);


                // read the size of reply and response message itself
                int contentSize = readStream.readInt();
//                System.out.println(contentSize);
                byte[] response = new byte[contentSize];
                readStream.readFully(response);
//                System.out.println(response);

                // and decode it
                this.commandManager.decode(response);
            }
        } catch (IOException | InterruptedException ex) {
            ex.getStackTrace();
        }
    }
}
