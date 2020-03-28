package lpi.client.soap;

import java.io.Closeable;
import java.net.MalformedURLException;
import java.net.URL;

public class ChatClient implements Closeable {

    private static final String DEFAULT_URL = "http://localhost:4321/chat?wsdl";

    private URL wsdlLocation;
    private IChatServer serverProxy;

    public ChatClient(String[] args) {
        // form the url of the web service
        try {
            if (args.length == 1) {
                wsdlLocation = new URL(args[0].trim());
            } else {
                wsdlLocation = new URL(DEFAULT_URL);
            }
        } catch (MalformedURLException e) {
            System.out.println(e.getMessage()+"\n");
        }
    }

    public void run() {
        try {
            // obtain a proxy object that allows to invoke methods on the server
            ChatServer serverWrapper = new ChatServer(wsdlLocation);
            serverProxy = serverWrapper.getChatServerProxy();

            ConnectionHandler connectionHandler = new ConnectionHandler(serverProxy);
            connectionHandler.run();
        } catch (Exception e) {
            System.out.println(e.getMessage()+"\n");
        }
    }

    public void close() {
        if (this.serverProxy != null) {
            this.serverProxy = null;
        }
    }
}