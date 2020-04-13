//package lpi.server.rest;
//
//import java.io.Closeable;
//import java.io.IOException;
//import java.time.Instant;
//import java.time.temporal.ChronoUnit;
//import java.util.Date;
//import java.util.Timer;
//import java.util.TimerTask;
//import java.util.UUID;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentMap;
//import java.util.stream.Collectors;
//
//import javax.jws.WebService;
//import javax.xml.ws.Endpoint;
//
//@WebService(serviceName = "ChatServer", portName = "ChatServerProxy", endpointInterface = "lpi.server.soap.IServer")
//public class SoapServer implements Runnable, Closeable {
//	
//	private static final String APPLICATION_NAME = "chat";
//	private static final String ALL_INTERFACES_ADDRESS = "0.0.0.0";
//	private static final String LOCALHOST_ADDRESS = "localhost";
//	
//	private static final long CLEANUP_DELAY_MS = 1000;
//	private static final long SESSION_TIME_SEC = 60 * 5;
//
//	private String hostname = "localhost";
//	private int port = 4321;
//
//	private Endpoint endpoint;
//
//	private ConcurrentMap<String, Instant> sessionToLastActionMap = new ConcurrentHashMap<>();
//	private ConcurrentMap<String, UserInfo> sessionToUserMap = new ConcurrentHashMap<>();
//	private ConcurrentMap<String, UserInfo> nameToUserMap = new ConcurrentHashMap<>();
//	private Timer sessionTimer = new Timer("Session Cleanup Timer", true);
//
//	public SoapServer(String[] args) {
//
//		if (args.length > 1) {
//			try {
//				this.hostname = args[0];
//				this.port = Integer.parseInt(args[1]);
//			} catch (Exception ex) {
//				// if we failed to parse port out of parameters, we'll just use
//				// the default one.
//			}
//		}
//	}
//
//	@Override
//	public void close() throws IOException {
//
//		if (this.endpoint != null) {
//			this.endpoint.stop();
//			this.endpoint = null;
//		}
//
//		if (this.sessionTimer != null) {
//			this.sessionTimer.cancel();
//			this.sessionTimer = null;
//		}
//	}
//
//	@Override
//	public void run() {
//
//		String address = String.format("http://%s:%s/%s", this.hostname, this.port, APPLICATION_NAME);
//
//		this.endpoint = Endpoint.publish(address, this);
//		this.sessionTimer.schedule(new SessionCleanupTask(), CLEANUP_DELAY_MS, CLEANUP_DELAY_MS);
//
//		// showing user-friendly message that explains how he can access the server.
//		String shownAddress = address;
//		if(this.hostname.equals(ALL_INTERFACES_ADDRESS))
//			shownAddress = address.replace(ALL_INTERFACES_ADDRESS, LOCALHOST_ADDRESS);
//		
//		System.out.printf("The SOAP server was started successfully at %s", shownAddress);
//		if(this.hostname.equals(ALL_INTERFACES_ADDRESS))
//			System.out.println(" (and on all ip addresses associated with this computer)");
//		else
//			System.out.println();
//		
//		System.out.printf("WSDL Schema is available at %s?wsdl%n", shownAddress);
//	}
//
//	public void ping() {
//		return; // simplest implementation possible.
//	}
//
//	public String echo(String text) {
//		return String.format("I'm glad to echo your message \"%s\", sir!", text);
//	}
//
//	public String login(String login, String password) throws ArgumentException, LoginException, ServerException {
//		try {
//			if (login == null || login.trim().length() == 0)
//				throw new ArgumentException("login", "Login can not be null or empty");
//
//			if (password == null || password.length() == 0)
//				throw new ArgumentException("password", "Password can not be null or empty");
//
//			String sessionId = UUID.randomUUID().toString();
//
//			UserInfo user = this.nameToUserMap.get(login);
//			if (user == null) {
//				UserInfo previousUser = this.nameToUserMap.putIfAbsent(login, user = new UserInfo(login, password));
//				if (previousUser != null)
//					user = previousUser;
//			}
//
//			if (!user.canLogin(login, password)) {
//				throw new LoginException("The login and password do not match");
//			}
//
//			sessionToLastActionMap.put(sessionId, Instant.now());
//			sessionToUserMap.put(sessionId, user);
//
//			System.out.printf("%s: User \"%s\" logged in. There are %s active users.%n", new Date(), login,
//					this.sessionToUserMap.size());
//
//			return sessionId;
//		} catch (Exception ex) {
//			throw new ServerException("Server failed to process your command", ex);
//		}
//	}
//
//	public String[] listUsers(String sessionId) throws ArgumentException, ServerException {
//		try {
//			ensureSessionValid(sessionId);
//
//			return this.sessionToUserMap.values().stream().map(user -> user.getLogin()).distinct().sorted()
//					.toArray(size -> new String[size]);
//		} catch (Exception ex) {
//			throw new ServerException("Server failed to process your command", ex);
//		}
//	}
//
//	public void sendMessage(String sessionId, Message msg) throws ArgumentException, ServerException {
//		try {
//			UserInfo user = ensureSessionValid(sessionId);
//
//			if (msg == null)
//				throw new ArgumentException("msg", "The message has to be specified");
//
//			if (msg.getReceiver() == null || msg.getReceiver().trim().length() == 0)
//				throw new ArgumentException("msg.receiver", "The message receiver has to be specified.");
//
//			UserInfo receiver = this.nameToUserMap.get(msg.getReceiver());
//			if (receiver == null)
//				throw new ArgumentException("msg.receiver", "There is no such receiver.");
//
//			msg.setSender(user.getLogin());
//			if (!receiver.addMessage(msg))
//				throw new ArgumentException("msg.receiver",
//						"The receiver can not receive your message now. Try sending it later, when he cleans up his message box.");
//		} catch (Exception ex) {
//			throw new ServerException("Server failed to process your command", ex);
//		}
//	}
//
//	public Message receiveMessage(String sessionId) throws ArgumentException, ServerException {
//		try {
//			UserInfo user = ensureSessionValid(sessionId);
//
//			return user.popMessage();
//
//		} catch (Exception ex) {
//			throw new ServerException("Server failed to process your command", ex);
//		}
//	}
//
//	public void sendFile(String sessionId, FileInfo file) throws ArgumentException, ServerException {
//		try {
//			UserInfo user = ensureSessionValid(sessionId);
//
//			if (file == null)
//				throw new ArgumentException("file", "The file has to be specified");
//
//			if (file.getReceiver() == null || file.getReceiver().trim().length() == 0)
//				throw new ArgumentException("file.receiver", "The file receiver has to be specified.");
//
//			file.setSender(user.getLogin());
//
//			UserInfo receiver = this.nameToUserMap.get(file.getReceiver());
//			if (receiver == null)
//				throw new ArgumentException("file.receiver", "There is no such receiver.");
//
//			if (!receiver.addFile(file))
//				throw new ArgumentException("file.receiver",
//						"The receiver can not receive your file now. Try sending it later, when he cleans up his message box.");
//
//		} catch (Exception ex) {
//			throw new ServerException("Server failed to process your command", ex);
//		}
//	}
//
//	@Override
//	public FileInfo receiveFile(String sessionId) throws ArgumentException, ServerException {
//		try {
//			UserInfo user = ensureSessionValid(sessionId);
//
//			return user.popFile();
//		} catch (Exception ex) {
//			throw new ServerException("Server failed to process your command", ex);
//		}
//	}
//
//	@Override
//	public void exit(String sessionId) throws ServerException {
//		try {
//			if (sessionId == null || sessionId.length() == 0)
//				return;
//			this.sessionToLastActionMap.remove(sessionId);
//			UserInfo user = this.sessionToUserMap.remove(sessionId);
//
//			if (user != null) {
//				System.out.printf("%s: User \"%s\" logged out. There are %s active users.%n", new Date(),
//						user.getLogin(), this.sessionToUserMap.size());
//			}
//
//		} catch (Exception ex) {
//			throw new ServerException("Server failed to process your command", ex);
//		}
//	}
//	
//	private UserInfo ensureSessionValid(String sessionId) throws ArgumentException {
//
//		if (sessionId == null || sessionId.length() == 0)
//			throw new ArgumentException("sessionId", "The provided session id is not valid");
//
//		UserInfo user = this.sessionToUserMap.get(sessionId);
//		if (user == null) {
//			throw new ArgumentException("sessionId",
//					String.format(
//							"The session id is not valid or expired. "
//									+ "Ensure you perform any operation with your session at least each %s seconds.",
//							SESSION_TIME_SEC));
//		}
//
//		// in case the user was removed right in between these actions, let's
//		// restore him there.
//		if (this.sessionToLastActionMap.put(sessionId, Instant.now()) == null)
//			this.sessionToUserMap.putIfAbsent(sessionId, user);
//
//		return user;
//	}
//
//	private class SessionCleanupTask extends TimerTask {
//		@Override
//		public void run() {
//			try {
//				Instant erasingPoint = Instant.now().minus(SESSION_TIME_SEC, ChronoUnit.SECONDS);
//
//				// removing all session older than erasing point, defined above.
//				sessionToLastActionMap.entrySet().stream().filter(entry -> entry.getValue().isBefore(erasingPoint))
//						.map(entry -> entry.getKey()).collect(Collectors.toList()).forEach((session -> {
//							sessionToLastActionMap.remove(session);
//							UserInfo user = sessionToUserMap.remove(session);
//							if (user != null) {
//								System.out.printf("%s: User's \"%s\" session expired. There are %s active users.%n",
//										new Date(), user.getLogin(), sessionToUserMap.size());
//							}
//						}));
//			} catch (Exception ex) {
//				ex.printStackTrace();
//			}
//		}
//	}
//}
