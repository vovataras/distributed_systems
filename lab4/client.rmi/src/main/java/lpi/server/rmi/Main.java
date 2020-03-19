package lpi.server.rmi;

public class Main {
    public static void main(String[] args) {

        try (RmiClient client = new RmiClient(args)) {
            client.run();
        }

        System.out.println("The client was shut down.");
    }
}
