package client.tcp;

import java.io.*;
import java.net.Socket;

public class TcpClient {
    private Socket socket = new Socket();
    private static final String DEFAULT_HOST = "0.0.0.0";
    private static final int DEFAULT_PORT = 4321;

    private String host = DEFAULT_HOST;
    private int port = DEFAULT_PORT;

    public TcpClient(String[] args) {

        if (args.length == 2) {
            host = args[0].trim();
            port = Integer.parseInt(args[1]);
        }

        System.out.printf("Hello, what do you want to do?");
    }

    public void run() throws IOException {
        this.socket = new Socket(host, port);
    }

    public void close() {
        if (this.socket != null) {
            try {
                this.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.socket = null;
        }
    }

}
