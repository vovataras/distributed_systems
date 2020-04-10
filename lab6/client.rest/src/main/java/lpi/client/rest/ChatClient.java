package lpi.client.rest;

import java.io.Closeable;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

public class ChatClient implements Closeable {

    private static final String DEFAULT_URL = "http://localhost:8080";

    private Client client;  // jersey REST client
    private final String targetURL;

    public ChatClient(String[] args) {
        // form the url of the web service
        String serverURL;
        if (args.length == 1) {
            serverURL = args[0].trim();
        } else {
            serverURL = DEFAULT_URL;
        }

        targetURL = serverURL + "/chat/server";
    }

    public void run() {
        client = ClientBuilder.newClient();

        try {
            // check whether the url is correct
            client.target(targetURL + "/ping")
                    .request(MediaType.TEXT_PLAIN_TYPE).get(String.class);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("No connection!" + "\n");
            return;
        }

        ConnectionHandler connectionHandler = new ConnectionHandler(client, targetURL);
        connectionHandler.run();
    }

    public void close() {
        if (client != null)
            client.close();
    }
}
