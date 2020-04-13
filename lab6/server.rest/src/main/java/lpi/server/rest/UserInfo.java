package lpi.server.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserInfo {
	private static final int MAX_PENDING_MESSAGES = 100;

	private final Object syncRoot = new Object();
	private final String login;
	private final String password;
	private final List<Message> messages = new ArrayList<>();
	private final List<FileInfo> files = new ArrayList<>();

	public UserInfo(String login, String password) {
		this.login = login;
		this.password = password;
	}

	public String getLogin() {
		return this.login;
	}

	public boolean canLogin(String login, String password) {
		return this.login.equals(login) && this.password.equals(password);
	}

	public boolean addMessage(Message msg) {
		synchronized (syncRoot) {
			if (this.messages.size() >= MAX_PENDING_MESSAGES)
				return false;

			this.messages.add(msg);
			return true;
		}
	}

	public boolean addFile(FileInfo file) {
		synchronized (syncRoot) {
			if (this.files.size() >= MAX_PENDING_MESSAGES)
				return false;

			this.files.add(file);
			return true;
		}
	}

	public String[] getMessages() {
		synchronized (syncRoot) {
			if (this.messages.size() == 0)
				return null;

			return this.messages.stream().map(msg -> msg.id).toArray(size -> new String[size]);
		}
	}

	public Message getMessage(String id) {
		synchronized (syncRoot) {
			return this.messages.stream().filter(msg -> msg.id.equals(id)).findFirst().orElse(null);
		}
	}

	public boolean removeMessage(String id) {
		synchronized (syncRoot) {
			return this.messages.remove(getMessage(id));
		}
	}

	public String[] getFiles(){
		synchronized (syncRoot) {
			if (this.files.size() == 0)
				return null;

			return this.files.stream().map(file -> file.id).toArray(size -> new String[size]);
		}
	}
	
	public FileInfo getFile(String id){
		synchronized (syncRoot) {
			return this.files.stream().filter(file -> file.id.equals(id)).findFirst().orElse(null);
		}
	}

	public boolean removeFile(String id) {
		synchronized (syncRoot) {
			return this.files.remove(getFile(id));
		}
	}
	
	public static class Message {
		public final String id = UUID.randomUUID().toString();
		public final String sender;
		public final String message;

		public Message(String sender, String message) {
			this.sender = sender;
			this.message = message;
		}
	}

	public static class FileInfo {
		public final String id = UUID.randomUUID().toString();
		public final String sender;
		public final String filename;
		public final String content;

		public FileInfo(String sender, String filename, String content) {
			this.sender = sender;
			this.filename = filename;
			this.content = content;
		}
	}
}
