package lpi.client.mq;

public interface QueueName {
    String ping  = "chat.diag.ping";
    String echo  = "chat.diag.echo";
    String exit  = "chat.exit";
    String login = "chat.login";
    String list  = "chat.listUsers";

    String sendMsg  = "chat.sendMessage";
    String sendFile = "chat.sendFile";
    String getMsg   = "chat.messages";
    String getFile  = "chat.chat.files";
}
