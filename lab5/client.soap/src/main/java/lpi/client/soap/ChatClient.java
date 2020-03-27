package lpi.client.soap;

import java.io.Closeable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class ChatClient implements Closeable {

//    private static final String DEFAULT_HOST = "0.0.0.0";
//    private static final int DEFAULT_PORT = 4321;
//
//    private String host = DEFAULT_HOST;
//    private int port = DEFAULT_PORT;

    private IChatServer serverProxy;

    public ChatClient(String[] args) {
//        if (args.length == 2) {
//            host = args[0].trim();
//            port = Integer.parseInt(args[1]);
//        }
    }

    public void run() {
        try {
            ChatServer serverWrapper = new ChatServer();
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