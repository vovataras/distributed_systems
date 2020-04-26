package lpi.client.mq;

public class Main {
    public static void main(String[] args) {
        try (ChatClient client = new ChatClient(args)) {
            client.run();
        }

        System.out.println("The client was shut down.");
    }
}