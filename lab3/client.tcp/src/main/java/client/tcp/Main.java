package client.tcp;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {

        TcpClient client = new TcpClient(args);

        try {
            client.run();
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (client != null){
            client.close();

        }
    }
}
