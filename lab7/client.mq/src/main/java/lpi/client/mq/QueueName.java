package lpi.client.mq;

public interface QueueName {
    String PING = "chat.diag.ping";
    String ECHO  = "chat.diag.echo";
    String EXIT  = "chat.exit";
    String LOGIN = "chat.login";
    String LIST  = "chat.listUsers";

    String SEND_MSG  = "chat.sendMessage";
    String SEND_FILE = "chat.sendFile";
    String GET_MSG   = "chat.messages";
    String GET_FILE  = "chat.files";
}
