package lpi.server.mq;

public class MessageInfo {
	public final String sender;
	public final String receiver;
	public final String message;
	
	public MessageInfo(String sender, String receiver, String message){
		this.sender = sender;
		this.receiver = receiver;
		this.message = message;
	}
}
