package server.tcp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;

import server.tcp.UserInfo.Message;

public class ProtocolManager {
	private static final Charset CHARSET = Charset.forName("UTF-8");

	// little bit more than 10MB
	private static final int MAX_CONTENT_SIZE = 10_500_000;

	// Commands
	private static final byte CMD_PING = 1;
	private static final byte CMD_PING_RESPONSE = 2;
	private static final byte CMD_ECHO = 3;
	private static final byte CMD_LOGIN = 5;
	private static final byte CMD_LIST = 10;
	private static final byte CMD_MSG = 15;
	private static final byte CMD_FILE = 20;
	private static final byte CMD_RECEIVE_MSG = 25;
	private static final byte CMD_RECEIVE_FILE = 30;

	private static final byte[] CMD_LOGIN_OK_NEW = new byte[] { 6 };
	private static final byte[] CMD_LOGIN_OK = new byte[] { 7 };
	private static final byte[] CMD_MSG_SENT = new byte[] { 16 };
	private static final byte[] CMD_FILE_SENT = new byte[] { 21 };
	private static final byte[] CMD_RECEIVE_MSG_EMPTY = new byte[] { 26 };
	private static final byte[] CMD_RECEIVE_FILE_EMPTY = new byte[] { 31 };

	// Errors
	private static final byte[] SERVER_ERROR = new byte[] { 100 };
	private static final byte[] INCORRECT_CONTENT_SIZE = new byte[] { 101 };
	private static final byte[] SERIALIZATION_ERROR = new byte[] { 102 };
	private static final byte[] INCORRECT_COMMAND = new byte[] { 103 };
	private static final byte[] WRONG_PARAMS = new byte[] { 104 };

	private static final byte[] LOGIN_WRONG_PASSWORD = new byte[] { 110 };
	private static final byte[] LOGIN_FIRST = new byte[] { 112 };
	private static final byte[] FAILED_SENDING = new byte[] { 113 };

	private IServer server;

	public ProtocolManager(IServer server) {
		this.server = server;
	}

	public void clientDisconnected(ConnectionHandler connectionHandler) {
		if (this.server != null)
			this.server.onClientDisconnected(connectionHandler);
	}

	public boolean isContentSizeOk(int contentSize) {
		return contentSize > 0 && contentSize < MAX_CONTENT_SIZE;
	}

	public byte[] execute(ConnectionHandler connectionHandler, byte[] command) {
		if (command.length == 0)
			return incorrectCommand();

		try {
			switch (command[0]) {
			case CMD_PING:
				return new byte[] { CMD_PING_RESPONSE };
			case CMD_ECHO:
				return getEchoResponse(command);
			case CMD_LOGIN:
				return handleLogin(connectionHandler, command);
			case CMD_LIST:
				return handleList(connectionHandler);
			case CMD_MSG:
				return handleMsg(connectionHandler, command);
			case CMD_FILE:
				return handleFile(connectionHandler, command);
			case CMD_RECEIVE_MSG:
				return handleReceiveMessage(connectionHandler);
			case CMD_RECEIVE_FILE:
				return handleReceiveFile(connectionHandler);

			default:
				return incorrectCommand();
			}
		} catch (Exception ex) {
			return SERVER_ERROR;
		}
	}

	public byte[] incorrectContentSize() {
		return INCORRECT_CONTENT_SIZE;
	}

	public byte[] serverError() {
		return SERVER_ERROR;
	}

	public byte[] serializationError() {
		return SERIALIZATION_ERROR;
	}

	public byte[] incorrectCommand() {
		return INCORRECT_COMMAND;
	}

	private byte[] getEchoResponse(byte[] command) {
		if (command.length < 2)
			return "ECHO: <empty message>".getBytes();

		try {
			String request = new String(command, 1, command.length - 1, CHARSET);

			return ("ECHO: " + request).getBytes();
		} catch (Exception ex) {
			return SERIALIZATION_ERROR;
		}
	}

	private byte[] handleLogin(ConnectionHandler connection, byte[] command) throws IOException {

		String[] params = null;
		try {
			params = deserialize(command, 1, String[].class);
		} catch (Exception ex) {
			return SERIALIZATION_ERROR;
		}

		if (params.length != 2 || params[0] == null || params[1] == null)
			return WRONG_PARAMS;

		boolean userExists = this.server.userExists(params[0]);

		boolean success = this.server.login(params[0], params[1], connection);

		if (success) {
			if (userExists)
				return CMD_LOGIN_OK;
			else
				return CMD_LOGIN_OK_NEW;
		} else
			return LOGIN_WRONG_PASSWORD;

	}

	private byte[] handleList(ConnectionHandler connectionHandler) {
		if (!this.server.isAuthenticated(connectionHandler))
			return LOGIN_FIRST;

		try {
			return serialize(this.server.getUsers());
		} catch (Exception ex) {
			return SERVER_ERROR;
		}
	}

	private byte[] handleMsg(ConnectionHandler connectionHandler, byte[] command) {
		if (!this.server.isAuthenticated(connectionHandler))
			return LOGIN_FIRST;

		String[] args = null;
		try {
			args = deserialize(command, 1, String[].class);
		} catch (Exception ex) {
			return SERIALIZATION_ERROR;
		}

		if (args.length != 2 || args[0] == null || args[1] == null)
			return WRONG_PARAMS;

		boolean succeed = this.server.sendMessage(connectionHandler, args[0], args[1]);

		return succeed ? CMD_MSG_SENT : FAILED_SENDING;
	}

	private byte[] handleFile(ConnectionHandler connectionHandler, byte[] command) {
		if (!this.server.isAuthenticated(connectionHandler))
			return LOGIN_FIRST;

		Object[] args = null;
		try {
			args = deserialize(command, 1, Object[].class);
		} catch (Exception ex) {
			return SERIALIZATION_ERROR;
		}

		if (args.length != 3 || args[0] == null || args[1] == null || args[2] == null
				|| !args[0].getClass().equals(String.class) || !args[1].getClass().equals(String.class)
				|| !args[2].getClass().equals(byte[].class))
			return WRONG_PARAMS;

		boolean succeed = this.server.sendFile(connectionHandler, (String) args[0], (String) args[1], (byte[]) args[2]);

		return succeed ? CMD_FILE_SENT : FAILED_SENDING;
	}

	private byte[] handleReceiveMessage(ConnectionHandler connectionHandler) throws IOException {
		if (!this.server.isAuthenticated(connectionHandler))
			return LOGIN_FIRST;

		Message msg = this.server.receiveMessage(connectionHandler);

		if (msg == null)
			return CMD_RECEIVE_MSG_EMPTY;

		return serialize(new String[] { msg.user, msg.message });
	}

	private byte[] handleReceiveFile(ConnectionHandler connectionHandler) throws IOException {
		if (!this.server.isAuthenticated(connectionHandler))
			return LOGIN_FIRST;

		Message msg = this.server.receiveFile(connectionHandler);

		if (msg == null)
			return CMD_RECEIVE_FILE_EMPTY;

		return serialize(new Object[] { msg.user, msg.message, msg.file });
	}

	private byte[] serialize(Object object) throws IOException {
		try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
				ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {
			objectStream.writeObject(object);
			return byteStream.toByteArray();
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T deserialize(byte[] data, int offset, Class<T> clazz) throws ClassNotFoundException, IOException {
		try (ByteArrayInputStream stream = new ByteArrayInputStream(data, offset, data.length - offset);
				ObjectInputStream objectStream = new ObjectInputStream(stream)) {
			return (T) objectStream.readObject();
		}
	}
}
