package server.tcp;

import java.io.Closeable;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import server.tcp.UserInfo.Message;

public class TcpServer implements Closeable, IServer {

	private static final int BACKLOG = 1024;
	private static final String DEFAULT_HOST = "localhost";
	private static final int DEFAULT_PORT = 4321;

	private String host = DEFAULT_HOST;
	private int port = DEFAULT_PORT;
	private ServerSocket socket;
	private volatile boolean isClosing = false;
	private Thread connectionHandler = null;
	private ProtocolManager protocolManager = new ProtocolManager(this);
	private List<ConnectionHandler> activeConnections = new CopyOnWriteArrayList<>();
	private ConcurrentHashMap<String, UserInfo> usersToInfo = new ConcurrentHashMap<>();
	private ConcurrentHashMap<ConnectionHandler, UserInfo> connectionsToInfo = new ConcurrentHashMap<>();

	public TcpServer(String[] args) {

		if (args.length == 2) {
			host = args[0].trim();
			port = Integer.parseInt(args[1]);
		}

		System.out.printf("Starting server on %s:%d...%n", host, port);
	}

	public void close() {
		this.isClosing = true;

		// TODO: cleanup here.
		for (ConnectionHandler connection : this.activeConnections) {
			connection.close();
		}
		
		this.usersToInfo.clear();
		this.connectionsToInfo.clear();

		// shutting down the socket.
		if (this.socket != null) {
			try {
				this.socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.socket = null;
		}
	}

	public void run() throws IOException {

		this.socket = new ServerSocket(this.port, BACKLOG, InetAddress.getByName(host));

		System.out.println("Waiting for incoming connections...");

		connectionHandler = new Thread(() -> {
			while (!isClosing) {
				try {
					Socket clientSocket = this.socket.accept();

					this.activeConnections.add(new ConnectionHandler(clientSocket, this.protocolManager));
					System.out.printf("%s: Incomming connection! %d active connections now.%n", new Date(),
							this.activeConnections.size());

				} catch (SocketException ex) {
					if (!this.isClosing)
						ex.printStackTrace();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		});
		connectionHandler.start();
	}

	@Override
	public void onClientDisconnected(ConnectionHandler connectionHandler) {
		this.activeConnections.remove(connectionHandler);
		
		UserInfo info = this.connectionsToInfo.remove(connectionHandler);
		if(info != null){
			info.logout(connectionHandler);
		}

		System.out.printf("%s: Connection closed. %d active connections remaining.%n", new Date(),
				this.activeConnections.size());
	}

	@Override
	public boolean userExists(String username) {
		return this.usersToInfo.containsKey(username);
	}

	@Override
	public boolean login(String username, String password, ConnectionHandler connection) {
		UserInfo newUserInfo = new UserInfo(username, password);
		UserInfo userInfo = this.usersToInfo.putIfAbsent(username, newUserInfo);

		if (userInfo == null)
			userInfo = newUserInfo;

		boolean loggedIn = userInfo.canLogin(username, password, connection);
		if (loggedIn)
			this.connectionsToInfo.put(connection, userInfo);
		
		return loggedIn;
	}

	@Override
	public boolean isAuthenticated(ConnectionHandler connection) {
		return this.connectionsToInfo.containsKey(connection);
	}

	@Override
	public String[] getUsers() {

		Set<String> activeUsers = new HashSet<>();

		for (UserInfo user : this.connectionsToInfo.values())
			activeUsers.add(user.getLogin());

		String[] activeUsersArray = activeUsers.toArray(new String[activeUsers.size()]);
		Arrays.sort(activeUsersArray);

		return activeUsersArray;
	}

	@Override
	public boolean sendMessage(ConnectionHandler connection, String dstUser, String message) {

		UserInfo senderInfo = this.connectionsToInfo.get(connection);
		if (senderInfo == null)
			return false;

		UserInfo userInfo = this.usersToInfo.get(dstUser);

		if (userInfo == null)
			return false;

		return userInfo.addMessage(senderInfo.getLogin(), message);
	}

	@Override
	public boolean sendFile(ConnectionHandler connection, String dstUser, String message, byte[] content) {
		UserInfo senderInfo = this.connectionsToInfo.get(connection);
		if (senderInfo == null)
			return false;

		UserInfo userInfo = this.usersToInfo.get(dstUser);

		if (userInfo == null)
			return false;

		return userInfo.addFile(senderInfo.getLogin(), message, content);
	}

	@Override
	public Message receiveMessage(ConnectionHandler connection) {
		UserInfo userInfo = this.connectionsToInfo.get(connection);
		if (userInfo == null)
			return null;

		return userInfo.popMessage();
	}

	@Override
	public Message receiveFile(ConnectionHandler connection) {
		UserInfo userInfo = this.connectionsToInfo.get(connection);
		if (userInfo == null)
			return null;

		return userInfo.popFile();
	}
}
