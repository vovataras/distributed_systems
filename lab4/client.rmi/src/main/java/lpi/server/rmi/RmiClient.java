package lpi.server.rmi;

import java.io.Closeable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RmiClient implements Closeable {

    private static final String DEFAULT_HOST = "0.0.0.0";
    private static final int DEFAULT_PORT = 4321;

    private String host = DEFAULT_HOST;
    private int port = DEFAULT_PORT;

    private IServer proxy;

    public RmiClient(String[] args) {
        if (args.length == 2) {
            host = args[0].trim();
            port = Integer.parseInt(args[1]);
        }
    }

    public void run() {
        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            proxy = (IServer) registry.lookup(IServer.RMI_SERVER_NAME);

            ConnectionHandler connectionHandler = new ConnectionHandler(this.proxy);
            connectionHandler.run();
        } catch (RemoteException | NotBoundException e) {
            System.out.println(e.getMessage()+"\n");
        }
    }

    public void close() {
        if (this.proxy != null) {
            this.proxy = null;
        }
    }
}
