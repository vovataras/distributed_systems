package lpi.client.mq;

import javax.jms.JMSException;
import java.io.Closeable;
import java.util.Arrays;
import javax.jms.Connection;
import javax.jms.Session;


public class ChatClient implements Closeable {

    private String hostname = "localhost";
    private int port = 61616;

    private Connection connection;
    private Session session;
    private Session sessionListener;

    public ChatClient(String[] args) {
        if (args.length == 2) {
            try {
                this.hostname = args[0];
                this.port = Integer.parseInt(args[1]);
            } catch (Exception ex) {
                // if failed to parse port out of parameters, just use the default one
            }
        }
    }


    public void run() {
        String brokerUrl = "tcp://" + this.hostname + ":" + this.port;
        boolean isTransact = false; // no transactions will be used.
        int ackMode = javax.jms.Session.AUTO_ACKNOWLEDGE; // automatically acknowledge.

        org.apache.activemq.ActiveMQConnectionFactory connectionFactory =
                new org.apache.activemq.ActiveMQConnectionFactory(brokerUrl);

        connectionFactory.setTrustedPackages(Arrays.asList("lpi.server.mq"));

        try {
            connection = connectionFactory.createConnection();
            connection.start();

            session = connection.createSession(isTransact, ackMode);
            sessionListener = connection.createSession(isTransact, ackMode);

            ConnectionHandler connectionHandler = new ConnectionHandler(session, sessionListener);
            connectionHandler.run();
        } catch (JMSException e) {
//            e.printStackTrace();
            System.out.println(e.getMessage() + "\n");
        }
    }


    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }

            if (session != null) {
                session.close();
            }

            if (sessionListener != null) {
                sessionListener.close();
            }
        } catch (JMSException e) {
//            e.printStackTrace();
            System.out.println(e.getMessage() + "\n");
        }
    }
}