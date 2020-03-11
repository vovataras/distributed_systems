package server.tcp;

import java.util.ArrayList;
import java.util.List;

public class UserInfo {
	private static final int MAX_PENDING_MESSAGES = 100;

	private final Object syncRoot = new Object();
	private final String login;
	private final String password;
	private final List<ConnectionHandler> activeConnections = new ArrayList<>();
	private final List<Message> messages = new ArrayList<>();
	private final List<Message> files = new ArrayList<>();

	public UserInfo(String login, String password) {
		this.login = login;
		this.password = password;
	}
	
	public String getLogin() {
		return this.login;
	}

	public boolean canLogin(String login, String password, ConnectionHandler connection) {
		synchronized (syncRoot) {
			if (this.login.equals(login) && this.password.equals(password)) {
				this.activeConnections.add(connection);
				return true;
			}

			return false;
		}
	}
	
	public void logout(ConnectionHandler connectionHandler) {
		synchronized (syncRoot) {
			this.activeConnections.remove(connectionHandler);
		}
	}


	public boolean addMessage(String user, String message) {
		synchronized (syncRoot) {
			if (this.messages.size() >= MAX_PENDING_MESSAGES)
				return false;

			this.messages.add(new Message(user, message));
			return true;
		}
	}

	public boolean addFile(String user, String message, byte[] file) {
		synchronized (syncRoot) {
			if (this.files.size() >= MAX_PENDING_MESSAGES)
				return false;

			this.files.add(new Message(user, message, file));
			return true;
		}
	}
	
	public Message popMessage(){
		synchronized (syncRoot) {
			if(this.messages.size() == 0)
				return null;
				
			return this.messages.remove(0);
		}
	}
	
	public Message popFile(){
		synchronized (syncRoot) {
			if(this.files.size() == 0)
				return null;
			
			return this.files.remove(0);
		}
	}

	public static class Message {
		final String user;
		final String message;
		final byte[] file;

		public Message(String user, String message) {
			this.user = user;
			this.message = message;
			this.file = null;
		}

		public Message(String user, String filename, byte[] content) {
			this.user = user;
			this.message = filename;
			this.file = content;
		}

		public boolean isMessage() {
			return this.file == null;
		}
	}


}
