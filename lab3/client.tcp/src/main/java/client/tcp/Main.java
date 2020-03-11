package client.tcp;


public class Main {
    public static void main(String[] args) throws Exception {

        try (TcpClient client = new TcpClient(args)) {
            client.run();
        }
    }
}
